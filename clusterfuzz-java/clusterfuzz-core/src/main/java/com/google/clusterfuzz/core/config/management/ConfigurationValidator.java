package com.google.clusterfuzz.core.config.management;

import com.google.clusterfuzz.core.entity.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Configuration validator for validating configuration values.
 * Based on ClusterFuzz configuration validation logic.
 */
@Component
@Slf4j
public class ConfigurationValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    );

    /**
     * Validates a configuration value.
     */
    public ValidationResult validateConfiguration(Configuration configuration) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        
        if (configuration == null) {
            result.setValid(false);
            result.addError("Configuration cannot be null");
            return result;
        }
        
        // Validate key
        validateKey(configuration.getKey(), result);
        
        // Validate value based on data type
        validateValue(configuration, result);
        
        // Validate constraints
        validateConstraints(configuration, result);
        
        return result;
    }

    /**
     * Validates configuration key.
     */
    private void validateKey(String key, ValidationResult result) {
        if (key == null || key.trim().isEmpty()) {
            result.setValid(false);
            result.addError("Configuration key is required");
            return;
        }
        
        // Check key format
        if (!key.matches("^[a-zA-Z0-9._-]+$")) {
            result.setValid(false);
            result.addError("Configuration key contains invalid characters. Only alphanumeric, dots, underscores, and hyphens are allowed");
        }
        
        // Check key length
        if (key.length() > 100) {
            result.setValid(false);
            result.addError("Configuration key is too long (max 100 characters)");
        }
    }

    /**
     * Validates configuration value based on data type.
     */
    private void validateValue(Configuration configuration, ValidationResult result) {
        String value = configuration.getValue();
        String dataType = configuration.getDataType();
        
        // Allow null values if default is provided
        if (value == null) {
            if (configuration.getDefaultValue() == null) {
                result.addWarning("Configuration has no value and no default value");
            }
            return;
        }
        
        if (dataType == null) {
            dataType = Configuration.DataType.STRING;
        }
        
        switch (dataType) {
            case Configuration.DataType.STRING:
                validateStringValue(value, result);
                break;
            case Configuration.DataType.INTEGER:
                validateIntegerValue(value, result);
                break;
            case Configuration.DataType.LONG:
                validateLongValue(value, result);
                break;
            case Configuration.DataType.DOUBLE:
                validateDoubleValue(value, result);
                break;
            case Configuration.DataType.BOOLEAN:
                validateBooleanValue(value, result);
                break;
            case Configuration.DataType.JSON:
                validateJsonValue(value, result);
                break;
            case Configuration.DataType.URL:
                validateUrlValue(value, result);
                break;
            case Configuration.DataType.EMAIL:
                validateEmailValue(value, result);
                break;
            case Configuration.DataType.PASSWORD:
                validatePasswordValue(value, result);
                break;
            default:
                result.addWarning("Unknown data type: " + dataType);
        }
    }

    /**
     * Validates configuration constraints.
     */
    private void validateConstraints(Configuration configuration, ValidationResult result) {
        String value = configuration.getValue();
        if (value == null) {
            return;
        }
        
        // Validate regex pattern
        if (configuration.getValidationRegex() != null) {
            validateRegexConstraint(value, configuration.getValidationRegex(), result);
        }
        
        // Validate allowed values
        if (configuration.getAllowedValues() != null) {
            validateAllowedValues(value, configuration.getAllowedValues(), result);
        }
        
        // Validate numeric constraints
        if (configuration.getDataType() != null && 
            (Configuration.DataType.INTEGER.equals(configuration.getDataType()) ||
             Configuration.DataType.LONG.equals(configuration.getDataType()) ||
             Configuration.DataType.DOUBLE.equals(configuration.getDataType()))) {
            validateNumericConstraints(value, configuration, result);
        }
    }

    /**
     * Validates string value.
     */
    private void validateStringValue(String value, ValidationResult result) {
        // Basic string validation
        if (value.length() > 10000) {
            result.setValid(false);
            result.addError("String value is too long (max 10000 characters)");
        }
    }

    /**
     * Validates integer value.
     */
    private void validateIntegerValue(String value, ValidationResult result) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            result.setValid(false);
            result.addError("Invalid integer value: " + value);
        }
    }

    /**
     * Validates long value.
     */
    private void validateLongValue(String value, ValidationResult result) {
        try {
            Long.parseLong(value);
        } catch (NumberFormatException e) {
            result.setValid(false);
            result.addError("Invalid long value: " + value);
        }
    }

    /**
     * Validates double value.
     */
    private void validateDoubleValue(String value, ValidationResult result) {
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            result.setValid(false);
            result.addError("Invalid double value: " + value);
        }
    }

    /**
     * Validates boolean value.
     */
    private void validateBooleanValue(String value, ValidationResult result) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            result.setValid(false);
            result.addError("Invalid boolean value: " + value + ". Must be 'true' or 'false'");
        }
    }

    /**
     * Validates JSON value.
     */
    private void validateJsonValue(String value, ValidationResult result) {
        try {
            // Basic JSON validation - check for balanced braces/brackets
            if (!isValidJsonStructure(value)) {
                result.setValid(false);
                result.addError("Invalid JSON structure");
            }
        } catch (Exception e) {
            result.setValid(false);
            result.addError("Invalid JSON value: " + e.getMessage());
        }
    }

    /**
     * Validates URL value.
     */
    private void validateUrlValue(String value, ValidationResult result) {
        try {
            new URL(value);
        } catch (MalformedURLException e) {
            result.setValid(false);
            result.addError("Invalid URL: " + e.getMessage());
        }
    }

    /**
     * Validates email value.
     */
    private void validateEmailValue(String value, ValidationResult result) {
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            result.setValid(false);
            result.addError("Invalid email format");
        }
    }

    /**
     * Validates password value.
     */
    private void validatePasswordValue(String value, ValidationResult result) {
        if (value.length() < 8) {
            result.addWarning("Password is shorter than recommended minimum (8 characters)");
        }
        
        if (value.length() > 128) {
            result.setValid(false);
            result.addError("Password is too long (max 128 characters)");
        }
        
        // Check for common weak passwords
        if (isWeakPassword(value)) {
            result.addWarning("Password appears to be weak");
        }
    }

    /**
     * Validates regex constraint.
     */
    private void validateRegexConstraint(String value, String regex, ValidationResult result) {
        try {
            Pattern pattern = Pattern.compile(regex);
            if (!pattern.matcher(value).matches()) {
                result.setValid(false);
                result.addError("Value does not match required pattern: " + regex);
            }
        } catch (PatternSyntaxException e) {
            result.addWarning("Invalid validation regex pattern: " + e.getMessage());
        }
    }

    /**
     * Validates allowed values constraint.
     */
    private void validateAllowedValues(String value, String allowedValues, ValidationResult result) {
        List<String> allowed = Arrays.asList(allowedValues.split(","));
        boolean found = allowed.stream()
            .anyMatch(allowedValue -> allowedValue.trim().equals(value));
        
        if (!found) {
            result.setValid(false);
            result.addError("Value '" + value + "' is not in allowed values: " + allowedValues);
        }
    }

    /**
     * Validates numeric constraints.
     */
    private void validateNumericConstraints(String value, Configuration configuration, ValidationResult result) {
        try {
            double numValue = Double.parseDouble(value);
            
            if (configuration.getMinValue() != null && numValue < configuration.getMinValue()) {
                result.setValid(false);
                result.addError("Value " + numValue + " is below minimum: " + configuration.getMinValue());
            }
            
            if (configuration.getMaxValue() != null && numValue > configuration.getMaxValue()) {
                result.setValid(false);
                result.addError("Value " + numValue + " is above maximum: " + configuration.getMaxValue());
            }
        } catch (NumberFormatException e) {
            // Already validated in type-specific validation
        }
    }

    /**
     * Checks if JSON structure is valid (basic check).
     */
    private boolean isValidJsonStructure(String json) {
        json = json.trim();
        
        if (json.isEmpty()) {
            return false;
        }
        
        // Check if it starts and ends with proper JSON delimiters
        if ((json.startsWith("{") && json.endsWith("}")) ||
            (json.startsWith("[") && json.endsWith("]")) ||
            json.startsWith("\"") && json.endsWith("\"") ||
            "true".equals(json) || "false".equals(json) || "null".equals(json)) {
            
            // Basic bracket/brace matching
            return isBalanced(json);
        }
        
        // Check if it's a number
        try {
            Double.parseDouble(json);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if brackets and braces are balanced.
     */
    private boolean isBalanced(String str) {
        int braces = 0;
        int brackets = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : str.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (inString) {
                continue;
            }
            
            switch (c) {
                case '{':
                    braces++;
                    break;
                case '}':
                    braces--;
                    if (braces < 0) return false;
                    break;
                case '[':
                    brackets++;
                    break;
                case ']':
                    brackets--;
                    if (brackets < 0) return false;
                    break;
            }
        }
        
        return braces == 0 && brackets == 0;
    }

    /**
     * Checks if password is weak.
     */
    private boolean isWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        
        // Common weak passwords
        String[] weakPasswords = {
            "password", "123456", "password123", "admin", "root", "user",
            "test", "guest", "demo", "qwerty", "abc123", "letmein"
        };
        
        for (String weak : weakPasswords) {
            if (lowerPassword.contains(weak)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private boolean valid = true;
        private List<String> errors = new java.util.ArrayList<>();
        private List<String> warnings = new java.util.ArrayList<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }

        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }

        public void addError(String error) {
            this.errors.add(error);
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}