package com.unishare.service;

import java.util.Arrays;
import java.util.List;

/**
 * Service for handling module-related operations
 */
public class ModuleService {
    
    private static final List<String> AVAILABLE_MODULES = Arrays.asList(
        "IN3111", "CS101", "MATH201", "PHYS202", "CHEM103"
    );
    
    public List<String> getAvailableModules() {
        return AVAILABLE_MODULES;
    }
    
    public boolean isValidModule(String module) {
        return AVAILABLE_MODULES.contains(module);
    }
    
    public String getModuleDescription(String module) {
        switch (module) {
            case "IN3111":
                return "Information Systems";
            case "CS101":
                return "Computer Science Fundamentals";
            case "MATH201":
                return "Advanced Mathematics";
            case "PHYS202":
                return "Physics II";
            case "CHEM103":
                return "Chemistry Fundamentals";
            default:
                return "Unknown Module";
        }
    }
}
