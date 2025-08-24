package com.laptoprepair.exception;

/**
 * Custom exception for CSV import errors.
 * This exception can carry information about the row number and field name
 * where the error occurred.
 */
public class CSVImportException extends Exception {

    private final int rowNumber;
    private final String fieldName;

    public CSVImportException(String message) {
        super(message);
        this.rowNumber = -1;
        this.fieldName = null;
    }

    public CSVImportException(String message, int rowNumber) {
        super(message);
        this.rowNumber = rowNumber;
        this.fieldName = null;
    }

    public CSVImportException(String message, int rowNumber, String fieldName) {
        super(message);
        this.rowNumber = rowNumber;
        this.fieldName = fieldName;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getFieldName() {
        return fieldName;
    }
}