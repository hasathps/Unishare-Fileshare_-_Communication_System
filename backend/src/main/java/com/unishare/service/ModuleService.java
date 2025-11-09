package com.unishare.service;

import com.unishare.model.ModuleInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service for handling module-related operations. Currently provides a static
 * catalog
 * which can later be replaced or merged with database-backed modules.
 */
public class ModuleService {

    /**
     * Titles provided by product requirements (landing page list).
     */
    private static final List<String> MODULE_TITLES = Arrays.asList(
            "Applied Numerical Methods",
            "Automata Theory",
            "Artificial Intelligence",
            "Enterprise Application Development",
            "Network programming",
            "Mobile Applications Development",
            "Embedded Systems",
            "Wireless Communication & Mobile Networks",
            "Human Computer Interaction",
            "Communication Skills and Professional Conduct",
            "Management Information Systems");

    /**
     * Build immutable list of ModuleInfo objects (code, title, description).
     * Code is a URL-safe slug of the title.
     */
    private static final List<ModuleInfo> MODULES;

    static {
        List<ModuleInfo> list = new ArrayList<>();
        for (String title : MODULE_TITLES) {
            String code = slugify(title);
            // For now description equals title; could be extended later.
            list.add(new ModuleInfo(code, title, title));
        }
        MODULES = List.copyOf(list);
    }

    public List<ModuleInfo> getModules() {
        return MODULES;
    }

    public ModuleInfo findByCode(String code) {
        if (code == null)
            return null;
        for (ModuleInfo info : MODULES) {
            if (info.getCode().equalsIgnoreCase(code)) {
                return info;
            }
        }
        return null;
    }

    public boolean isValidModule(String code) {
        return findByCode(code) != null;
    }

    public String getModuleDescription(String code) {
        ModuleInfo info = findByCode(code);
        return info != null ? info.getDescription() : "Unknown Module";
    }

    private static String slugify(String title) {
        String lower = title.toLowerCase(Locale.ENGLISH);
        // Replace ampersand with 'and' for consistency
        lower = lower.replace("&", " and ");
        // Keep alphanumeric and spaces
        lower = lower.replaceAll("[^a-z0-9 ]", "");
        // Collapse multiple spaces
        lower = lower.replaceAll(" +", "-");
        return lower;
    }

    /**
     * Convenience: return just module codes (used by legacy callers).
     */
    public List<String> getAvailableModules() {
        return MODULES.stream().map(ModuleInfo::getCode).collect(Collectors.toList());
    }
}
