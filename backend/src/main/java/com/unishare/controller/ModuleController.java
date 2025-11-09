package com.unishare.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.unishare.model.FileInfo;
import com.unishare.model.ModuleInfo;
import com.unishare.service.FileService;
import com.unishare.service.ModuleService;
import com.unishare.util.CORSFilter;
import java.io.IOException;
import java.util.List;

/**
 * Controller for handling module-related requests
 */
public class ModuleController implements HttpHandler {

    private final ModuleService moduleService;
    private final FileService fileService;

    public ModuleController(ModuleService moduleService) {
        this(moduleService, null);
    }

    public ModuleController(ModuleService moduleService, FileService fileService) {
        this.moduleService = moduleService;
        this.fileService = fileService; // may be null if not wired yet
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            switch (method) {
                case "GET":
                    if (path.equals("/api/modules")) {
                        handleModules(exchange);
                    } else if (path.startsWith("/api/modules/")) {
                        handleModuleFiles(exchange);
                    }
                    break;
                case "OPTIONS":
                    CORSFilter.handlePreflightRequest(exchange);
                    break;
                default:
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            System.err.println("Error handling module request: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }

    public void handleModules(HttpExchange exchange) throws IOException {
        try {
            List<ModuleInfo> modules = moduleService.getModules();

            // Optional enrichment with file counts if fileService present
            if (fileService != null) {
                for (ModuleInfo info : modules) {
                    try {
                        List<FileInfo> files = fileService.getFilesForModule(info.getCode());
                        info.setFileCount(files.size());
                    } catch (Exception e) {
                        // Non-fatal; leave count at zero
                    }
                }
            }

            StringBuilder json = new StringBuilder("{\"modules\":[");
            for (int i = 0; i < modules.size(); i++) {
                ModuleInfo m = modules.get(i);
                if (i > 0)
                    json.append(",");
                json.append("{\"code\":\"").append(m.getCode()).append("\",")
                        .append("\"name\":\"").append(escape(m.getName())).append("\",")
                        .append("\"description\":\"").append(escape(m.getDescription())).append("\",")
                        .append("\"fileCount\":").append(m.getFileCount()).append("}");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes();
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (Exception e) {
            System.err.println("❌ Failed to get modules: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to get modules");
        }
    }

    public void handleModuleFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String moduleCode = path.substring("/api/modules/".length());

        try {
            if (!moduleService.isValidModule(moduleCode)) {
                sendErrorResponse(exchange, 404, "Module not found");
                return;
            }

            List<FileInfo> files = fileService != null ? fileService.getFilesForModule(moduleCode) : List.of();
            ModuleInfo moduleInfo = moduleService.findByCode(moduleCode);

            StringBuilder json = new StringBuilder();
            json.append("{\"module\":{");
            json.append("\"code\":\"").append(moduleInfo.getCode()).append("\",");
            json.append("\"name\":\"").append(escape(moduleInfo.getName())).append("\",");
            json.append("\"description\":\"").append(escape(moduleInfo.getDescription())).append("\"");
            json.append("},\"files\":[");
            for (int i = 0; i < files.size(); i++) {
                if (i > 0)
                    json.append(",");
                json.append(files.get(i).toJson());
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes();
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (Exception e) {
            System.err.println("❌ Failed to get module files: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to get module files");
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int code, String message) throws IOException {
        String response = "{\"error\":\"" + message + "\"}";
        CORSFilter.addCORSHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\"", "\\\"");
    }
}
