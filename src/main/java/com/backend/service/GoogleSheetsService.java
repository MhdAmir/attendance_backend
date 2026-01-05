package com.backend.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GoogleSheetsService {
    
    @Value("${google.sheets.spreadsheet.id}")
    private String spreadsheetId;
    
    @Value("${google.sheets.credentials.path}")
    private String credentialsPath;
    
    private static final String APPLICATION_NAME = "Eros Attendance System";
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/spreadsheets");
    
    /**
     * Get Sheets service
     */
    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createScoped(SCOPES);
        
        return new Sheets.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    
    /**
     * Get or create sheet for specific month
     */
    public String getOrCreateMonthSheet(int year, int month) throws IOException, GeneralSecurityException {
        String sheetName = String.format("%d-%02d", year, month);
        Sheets service = getSheetsService();
        
        // Check if sheet exists
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheet.getProperties().getTitle().equals(sheetName)) {
                return sheetName;
            }
        }
        
        // Create new sheet
        AddSheetRequest addSheetRequest = new AddSheetRequest()
                .setProperties(new SheetProperties().setTitle(sheetName));
        
        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(Collections.singletonList(new Request().setAddSheet(addSheetRequest)));
        
        service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();
        
        // Add header row
        List<List<Object>> headers = Arrays.asList(
                Arrays.asList("Full Name", "Check In", "Check Out", "Duration", "Status")
        );
        
        ValueRange body = new ValueRange().setValues(headers);
        service.spreadsheets().values()
                .update(spreadsheetId, sheetName + "!A1:E1", body)
                .setValueInputOption("RAW")
                .execute();
        
        // Format header
        formatHeader(service, spreadsheet.getSheets().size() - 1);
        
        return sheetName;
    }
    
    /**
     * Write attendance to Google Sheets
     */
    public void writeAttendance(String fullName, LocalDateTime checkIn, LocalDateTime checkOut) 
            throws IOException, GeneralSecurityException {
        
        int year = checkIn.getYear();
        int month = checkIn.getMonthValue();
        String sheetName = getOrCreateMonthSheet(year, month);
        
        Sheets service = getSheetsService();
        
        // Format times
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String checkInStr = checkIn.format(formatter);
        String checkOutStr = checkOut != null ? checkOut.format(formatter) : "-";
        
        // Calculate duration
        String duration = "-";
        String status = "Checked In";
        if (checkOut != null) {
            long minutes = java.time.Duration.between(checkIn, checkOut).toMinutes();
            long hours = minutes / 60;
            long mins = minutes % 60;
            duration = String.format("%d:%02d", hours, mins);
            status = "Completed";
        }
        
        // Append row
        List<List<Object>> values = Arrays.asList(
                Arrays.asList(fullName, checkInStr, checkOutStr, duration, status)
        );
        
        ValueRange body = new ValueRange().setValues(values);
        service.spreadsheets().values()
                .append(spreadsheetId, sheetName + "!A:E", body)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
        
        // Update monthly summary with checkmark
        updateMonthlySummary(fullName, checkIn);
    }
    
    /**
     * Update existing attendance record
     */
    public void updateAttendance(String fullName, LocalDateTime checkIn, LocalDateTime checkOut) 
            throws IOException, GeneralSecurityException {
        
        int year = checkIn.getYear();
        int month = checkIn.getMonthValue();
        String sheetName = String.format("%d-%02d", year, month);
        
        Sheets service = getSheetsService();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String checkInStr = checkIn.format(formatter);
        
        // Find the row with matching name and check-in time
        ValueRange result = service.spreadsheets().values()
                .get(spreadsheetId, sheetName + "!A:E")
                .execute();
        
        List<List<Object>> values = result.getValues();
        if (values == null || values.isEmpty()) {
            writeAttendance(fullName, checkIn, checkOut);
            return;
        }
        
        int rowIndex = -1;
        for (int i = 1; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() >= 2 && 
                row.get(0).toString().equals(fullName) && 
                row.get(1).toString().equals(checkInStr)) {
                rowIndex = i + 1; // +1 because sheets are 1-indexed
                break;
            }
        }
        
        if (rowIndex == -1) {
            writeAttendance(fullName, checkIn, checkOut);
            return;
        }
        
        // Update check-out and duration
        String checkOutStr = checkOut.format(formatter);
        long minutes = java.time.Duration.between(checkIn, checkOut).toMinutes();
        long hours = minutes / 60;
        long mins = minutes % 60;
        String duration = String.format("%d:%02d", hours, mins);
        
        List<List<Object>> updateValues = Arrays.asList(
                Arrays.asList(checkOutStr, duration, "Completed")
        );
        
        ValueRange body = new ValueRange().setValues(updateValues);
        String range = String.format("%s!C%d:E%d", sheetName, rowIndex, rowIndex);
        
        service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute();
    }
    
    /**
     * Format header row
     */
    private void formatHeader(Sheets service, int sheetId) throws IOException {
        List<Request> requests = new ArrayList<>();
        
        // Bold header
        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(0)
                        .setEndRowIndex(1))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                        .setTextFormat(new TextFormat().setBold(true))
                        .setBackgroundColor(new Color()
                                .setRed(0.85f)
                                .setGreen(0.85f)
                                .setBlue(0.85f))))
                .setFields("userEnteredFormat(textFormat,backgroundColor)")));
        
        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(requests);
        
        service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();
    }
    
    /**
     * Get or create monthly summary sheet with checkmarks
     */
    public String getOrCreateMonthlySummarySheet(int year, int month) throws IOException, GeneralSecurityException {
        String sheetName = String.format("Summary-%d-%02d", year, month);
        Sheets service = getSheetsService();
        
        // Check if sheet exists
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheet.getProperties().getTitle().equals(sheetName)) {
                return sheetName;
            }
        }
        
        // Create new sheet
        AddSheetRequest addSheetRequest = new AddSheetRequest()
                .setProperties(new SheetProperties().setTitle(sheetName));
        
        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(Collections.singletonList(new Request().setAddSheet(addSheetRequest)));
        
        service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();
        
        // Create header with dates
        int daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();
        List<Object> headers = new ArrayList<>();
        headers.add("Name");
        for (int day = 1; day <= daysInMonth; day++) {
            headers.add(String.valueOf(day));
        }
        headers.add("Total");
        
        List<List<Object>> headerValues = Collections.singletonList(headers);
        ValueRange body = new ValueRange().setValues(headerValues);
        service.spreadsheets().values()
                .update(spreadsheetId, sheetName + "!A1", body)
                .setValueInputOption("RAW")
                .execute();
        
        // Format header for summary sheet
        Sheet createdSheet = null;
        spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheet.getProperties().getTitle().equals(sheetName)) {
                createdSheet = sheet;
                break;
            }
        }
        
        if (createdSheet != null) {
            formatSummaryHeader(service, createdSheet.getProperties().getSheetId());
        }
        
        return sheetName;
    }
    
    /**
     * Update monthly summary sheet with attendance
     */
    public void updateMonthlySummary(String fullName, LocalDateTime checkIn) 
            throws IOException, GeneralSecurityException {
        
        int year = checkIn.getYear();
        int month = checkIn.getMonthValue();
        int day = checkIn.getDayOfMonth();
        
        String sheetName = getOrCreateMonthlySummarySheet(year, month);
        Sheets service = getSheetsService();
        
        // Get existing data
        ValueRange result = service.spreadsheets().values()
                .get(spreadsheetId, sheetName + "!A:ZZ")
                .execute();
        
        List<List<Object>> values = result.getValues();
        if (values == null) {
            values = new ArrayList<>();
        }
        
        // Find or create row for this user
        int rowIndex = -1;
        for (int i = 1; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (!row.isEmpty() && row.get(0).toString().equals(fullName)) {
                rowIndex = i + 1; // +1 because sheets are 1-indexed
                break;
            }
        }
        
        if (rowIndex == -1) {
            // Create new row for user
            int daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();
            List<Object> newRow = new ArrayList<>();
            newRow.add(fullName);
            for (int i = 0; i < daysInMonth; i++) {
                newRow.add("");
            }
            newRow.add("0");
            
            List<List<Object>> newRowValues = Collections.singletonList(newRow);
            ValueRange body = new ValueRange().setValues(newRowValues);
            service.spreadsheets().values()
                    .append(spreadsheetId, sheetName + "!A:ZZ", body)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
            
            rowIndex = values.size() + 1;
        }
        
        // Update checkmark for the day (day is in column B=1, C=2, etc.)
        // Column A is for Name, so day 1 should be in column B (index 2), day 2 in column C (index 3), etc.
        String columnLetter = getColumnLetter(day + 1); // +1 because column A is for Name
        String range = String.format("%s!%s%d", sheetName, columnLetter, rowIndex);
        
        List<List<Object>> checkmarkValues = Collections.singletonList(
                Collections.singletonList("✓")
        );
        
        ValueRange body = new ValueRange().setValues(checkmarkValues);
        service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute();
        
        // Update total count
        updateTotalAttendance(service, sheetName, rowIndex);
    }
    
    /**
     * Format summary header
     */
    private void formatSummaryHeader(Sheets service, int sheetId) throws IOException {
        List<Request> requests = new ArrayList<>();
        
        // Bold and color header
        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(0)
                        .setEndRowIndex(1))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                        .setTextFormat(new TextFormat().setBold(true))
                        .setHorizontalAlignment("CENTER")
                        .setBackgroundColor(new Color()
                                .setRed(0.2f)
                                .setGreen(0.6f)
                                .setBlue(0.9f))))
                .setFields("userEnteredFormat(textFormat,backgroundColor,horizontalAlignment)")));
        
        // Freeze first row and first column
        requests.add(new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
                .setProperties(new SheetProperties()
                        .setSheetId(sheetId)
                        .setGridProperties(new GridProperties()
                                .setFrozenRowCount(1)
                                .setFrozenColumnCount(1)))
                .setFields("gridProperties.frozenRowCount,gridProperties.frozenColumnCount")));
        
        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(requests);
        
        service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();
    }
    
    /**
     * Update total attendance count for a user
     */
    private void updateTotalAttendance(Sheets service, String sheetName, int rowIndex) 
            throws IOException {
        
        // Get the row data to count checkmarks
        ValueRange result = service.spreadsheets().values()
                .get(spreadsheetId, String.format("%s!B%d:AJ%d", sheetName, rowIndex, rowIndex))
                .execute();
        
        List<List<Object>> values = result.getValues();
        int count = 0;
        if (values != null && !values.isEmpty()) {
            List<Object> row = values.get(0);
            for (Object cell : row) {
                if (cell != null && cell.toString().equals("✓")) {
                    count++;
                }
            }
        }
        
        // Update total column (assuming last column)
        List<List<Object>> totalValues = Collections.singletonList(
                Collections.singletonList(count)
        );
        
        // Find last column based on days in month
        ValueRange headerResult = service.spreadsheets().values()
                .get(spreadsheetId, sheetName + "!A1:ZZ1")
                .execute();
        
        int lastColIndex = 0;
        if (headerResult.getValues() != null && !headerResult.getValues().isEmpty()) {
            lastColIndex = headerResult.getValues().get(0).size() - 1;
        }
        
        String lastColumnLetter = getColumnLetter(lastColIndex + 1);
        String range = String.format("%s!%s%d", sheetName, lastColumnLetter, rowIndex);
        
        ValueRange body = new ValueRange().setValues(totalValues);
        service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute();
    }
    
    /**
     * Convert column number to letter (1=A, 2=B, ..., 27=AA, etc.)
     */
    private String getColumnLetter(int column) {
        StringBuilder result = new StringBuilder();
        while (column > 0) {
            int remainder = (column - 1) % 26;
            result.insert(0, (char) ('A' + remainder));
            column = (column - 1) / 26;
        }
        return result.toString();
    }
}
