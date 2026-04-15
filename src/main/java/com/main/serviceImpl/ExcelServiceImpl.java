package com.main.serviceImpl;


import com.main.service.ExcelService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.main.util.ConstKeyStrings.*;

@Component
public class ExcelServiceImpl implements ExcelService {

    String englishPattern = "^[A-Za-z\\p{P}\\p{S}\\p{Z}]*$";


    @Override
    public byte[] generateExcelFromJson(Map<String, Object> jsonResponse) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Patent Data");

        Row headerRow = sheet.createRow(0);
        String[] columns = {"S No", "Publication Number",
                "Title", "Abstract", "Application number", "Earliest Priority", "Filing Application Date",
                "Publication Date", "Protected authorities - Country codes", "Earliest application date",
                "Inventors", "IPC Classes", "ASSIGNEE", "Priority Country", "Legal status", "CPC Classes", "Current Owners", "Current Assignees", "Family Id",
                "Publication Type", "isLicensed", "isLitigated", "Legal Events"};

        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(style);
        }

        Map<String, Object> response = (Map<String, Object>) jsonResponse.get(RESPONSE);
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

        int rowNum = 1;
        for (Map<String, Object> item : items) {

            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(getStringValue(item.get(DOCUMENT_NUMBER)));


            if (item.containsKey("inventionTitle")) {
                row.createCell(2).setCellValue(getStringValue(item.get("inventionTitle")));
            } else if (item.containsKey("inventionTitles")) {
                Object titlesObj = item.get("inventionTitles");

                if (titlesObj instanceof List) {
                    List<Map<String, Object>> inventionTitles = (List<Map<String, Object>>) titlesObj;

                    if (!inventionTitles.isEmpty()) {
                        String inventionTitleName = getStringValue(inventionTitles.get(0).get("title"));
                        row.createCell(2).setCellValue(inventionTitleName);
                    } else {row.createCell(2).setCellValue("");
                    }
                } else {row.createCell(2).setCellValue("");
                }
            } else {row.createCell(2).setCellValue("");
            }
            List<Map<String, Object>> abstracts = (List<Map<String, Object>>) item.get("abstractParagraphs");
            String abstractName = "";
            if (abstracts != null && !abstracts.isEmpty()) {
                for (Map<String, Object> abstractEntry : abstracts) {
                    if ("en".equals(abstractEntry.get("lang"))) {
                        abstractName = getStringValue(abstractEntry.get("plainText"));
                        break;
                    }
                }
            }
            row.createCell(3).setCellValue(abstractName);


            Map<String, Object> applicationReference = (Map<String, Object>) item.get("applicationReference");
            String applicationNumbers = "";

            if (applicationReference != null) {
                String country = getStringValue(applicationReference.get("country"));
                String documentNumber = getStringValue(applicationReference.get("documentNumber"));
                if (country != null && documentNumber != null) {
                    applicationNumbers = country + documentNumber;
                }
            }

            row.createCell(4).setCellValue(applicationNumbers);


            row.createCell(5).setCellValue(getStringValue(item.get("minPriorityDate")));

            Map<String, Object> applicationFilingDate = (Map<String, Object>) item.get("applicationReference");
            String applicationFilingDates = "";
            if (applicationFilingDate != null && applicationFilingDate.containsKey("documentDate")) {
                applicationFilingDates = getStringValue(applicationFilingDate.get("documentDate"));
            }
            row.createCell(6).setCellValue(applicationFilingDates);


            Map<String, Object> publicationDate = (Map<String, Object>) item.get("publicationReference");
            String publicationDates = "";
            if (publicationDate != null && publicationDate.containsKey("documentDate")) {
                publicationDates = getStringValue(publicationDate.get("documentDate"));
            }

            row.createCell(7).setCellValue(publicationDates);
            List<Map<String, Object>> protectedAuthorities = (List<Map<String, Object>>) item.get("inpadocFamilyMembers");
            Set<String> uniqueCountryCodes = new HashSet<>();
            if (protectedAuthorities != null && !protectedAuthorities.isEmpty()) {
                for (Map<String, Object> member : protectedAuthorities) {
                    String country = (String) member.get("country");
                    if (country != null) {
                        uniqueCountryCodes.add(country);
                    }
                }
            }

            String countryCodes = String.join("|", uniqueCountryCodes);
            row.createCell(8).setCellValue(countryCodes);

            List<Map<String, Object>> earliestApplicationDate = (List<Map<String, Object>>) item.get("applicationReferences");
            String earliestApplicationDates = "";
            if (earliestApplicationDate != null && !earliestApplicationDate.isEmpty()) {
                for (Map<String, Object> reference : earliestApplicationDate) {
                    if (reference.containsKey("documentDate")) {
                        earliestApplicationDates = getStringValue(reference.get("documentDate"));
                        break;
                    }
                }
            }
            row.createCell(9).setCellValue(earliestApplicationDates);


            List<Map<String, Object>> inventors = (List<Map<String, Object>>) item.get("inventors");
            Set<String> inventorsNames = new HashSet<>();
            if (inventors != null && !inventors.isEmpty()) {
                for (Map<String, Object> member : inventors) {
                    String names = (String) member.get("partyNameClean");
                    if (names == null) {
                        names = (String) member.get("partyName");
                    }
                    if (names != null && names.matches(englishPattern)) {
                        inventorsNames.add(names);
                    }
                }
            }

            String inventor = String.join("|", inventorsNames);
            row.createCell(10).setCellValue(inventor);

            List<Map<String, Object>> ipcClass = (List<Map<String, Object>>) item.get("ipcClassifications");
            Set<String> ipcClasses = new HashSet<>();
            if (ipcClass != null && !ipcClass.isEmpty()) {
                for (Map<String, Object> member : ipcClass) {
                    String ipcCode = (String) member.get("symbol");
                    if (ipcCode != null) {
                        ipcClasses.add(ipcCode);
                    }
                }
            }

            String ipcCodes = String.join("|", ipcClasses);
            row.createCell(11).setCellValue(ipcCodes);


            List<Map<String, Object>> assignees = (List<Map<String, Object>>) item.get("assignees");
            Set<String> assigneesNames = new HashSet<>();
            if (assignees != null && !assignees.isEmpty()) {
                for (Map<String, Object> member : assignees) {
                    String names = (String) member.get("partyNameClean");
                    if (names == null) {
                        names = (String) member.get("partyName");
                    }
                    if (names != null && names.matches(englishPattern)) {
                        assigneesNames.add(names);
                    }
                }
            }

            String assignee = String.join("|", assigneesNames);
            row.createCell(12).setCellValue(assignee);



            List<Map<String, Object>> priorityCountry = (List<Map<String, Object>>) item.get("priorityClaims");
            Set<String> priorityCountryCodes = new HashSet<>();
            if (priorityCountry != null && !priorityCountry.isEmpty()) {
                for (Map<String, Object> member : priorityCountry) {
                    String countryCode = (String) member.get("country");
                    if (countryCode != null) {
                        priorityCountryCodes.add(countryCode);
                    }
                }
            }

            String codes = String.join("|", priorityCountryCodes);
            row.createCell(13).setCellValue(codes);

            row.createCell(14).setCellValue(getStringValue(item.get("legalStatus")));




            List<Map<String, Object>> cpcClass = (List<Map<String, Object>>) item.get("cpcClassifications");
            Set<String> cpcClasses = new HashSet<>();
            if (cpcClass != null && !cpcClass.isEmpty()) {
                for (Map<String, Object> member : cpcClass) {
                    String cpcCode = (String) member.get("symbol");
                    if (cpcCode != null) {
                        cpcClasses.add(cpcCode);
                    }
                }
            }

            String cpcCodes = String.join("|", cpcClasses);
            row.createCell(15).setCellValue(cpcCodes);

            List<Map<String, Object>> currentOwners = (List<Map<String, Object>>) item.get("currentOwners");
            Set<String> currentOwnersNames = new HashSet<>();
            if (currentOwners != null && !currentOwners.isEmpty()) {
                for (Map<String, Object> member : currentOwners) {
                    String names = (String) member.get("partyNameClean");
                    if (names == null) {
                        names = (String) member.get("partyName");
                    }
                    if (names != null && names.matches(englishPattern)) {
                        currentOwnersNames.add(names);
                    }
                }
            }

            String currentOwner = String.join("|", currentOwnersNames);
            row.createCell(16).setCellValue(currentOwner);


            List<Map<String, Object>> currentAssignee = (List<Map<String, Object>>) item.get("currentAssignees");
            Set<String> currentAssigneeNames = new HashSet<>();
            if (currentAssignee != null && !currentAssignee.isEmpty()) {
                for (Map<String, Object> member : currentAssignee) {
                    String names = (String) member.get("partyNameClean");
                    if (names == null) {
                        names = (String) member.get("partyName");
                    }
                    if (names != null && names.matches(englishPattern)) {
                        currentAssigneeNames.add(names);
                    }
                }
            }

            String currentAssignees = String.join("|", currentAssigneeNames);
            row.createCell(17).setCellValue(currentAssignees);


            row.createCell(18).setCellValue(getStringValue(item.get("familyId")));

            row.createCell(19).setCellValue(getStringValue(item.get("publicationType")));

            row.createCell(20).setCellValue(getStringValue(item.get("isLicensed")));

            row.createCell(21).setCellValue(getStringValue(item.get("isLitigated")));

            List<Map<String, Object>> legalEvents = (List<Map<String, Object>>) item.get("legalEvents");
            Set<String> legalEvent = new HashSet<>();
            if (legalEvents != null && !legalEvents.isEmpty()) {
                for (Map<String, Object> member : legalEvents) {
                    String events = (String) member.get("englishDescription");
                    if (events != null) {
                        legalEvent.add(events);
                    }
                }
            }

            String legalEventss = String.join("|", legalEvent);
            row.createCell(22).setCellValue(legalEventss);

            List<Map<String, Object>> claims = (List<Map<String, Object>>) item.get("claims");
            Map<String, StringBuilder> claimTextMap = new LinkedHashMap<>();

            if (claims != null && !claims.isEmpty()) {
                for (Map<String, Object> claim : claims) {
                    String claimId = getStringValue(claim.get("claimId"));
                    String plainText = getStringValue(claim.get("plainText"));
                    String language = getStringValue(claim.get("lang"));

                    if ("en".equals(language)) {
                        claimTextMap.putIfAbsent(claimId, new StringBuilder());

                        if (claimTextMap.get(claimId).length() > 0) {
                            claimTextMap.get(claimId).append(" | ");
                        }
                        claimTextMap.get(claimId).append(plainText);
                    }
                }
            }

            int columnIndex = 23;
            int claimCounter = 1;

            for (String claimId : claimTextMap.keySet()) {
                Cell cell = headerRow.createCell(columnIndex++);
                cell.setCellValue("Claims - " + claimCounter++);
                cell.setCellStyle(style);
            }

            columnIndex = 23;
            for (StringBuilder claimText : claimTextMap.values()) {
                row.createCell(columnIndex++).setCellValue(claimText.toString());
            }

        }


        for (int i = 0; i < columns.length; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private String getStringValue(Object value) {
        return (value != null) ? value.toString() : "";
    }


    @Override
    public List<String> extractDocumentNumbers(MultipartFile file) throws IOException {
        List<String> documentNumbers = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell = row.getCell(0);

            if (cell != null && cell.getCellType() == CellType.STRING) {
                String cellValue = cell.getStringCellValue().trim();
                if (!cellValue.isEmpty()) {
                    documentNumbers.add(cellValue);
                }
            }
        }
        workbook.close();
        return documentNumbers;
    }


    @Override
    public boolean isExcelEmpty(byte[] excelData) throws IOException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(excelData)) {
            Workbook workbook = new XSSFWorkbook(byteArrayInputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() > 0) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public byte[] createZipFromPdfFiles(List<byte[]> pdfFiles, List<String> fileNames) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (int i = 0; i < pdfFiles.size(); i++) {
                zos.putNextEntry(new ZipEntry(fileNames.get(i) + ".pdf"));
                zos.write(pdfFiles.get(i));
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }


    @Override
    public byte[] createZipFromPNGs(Map<String, List<byte[]>> files) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (Map.Entry<String, List<byte[]>> entry : files.entrySet()) {
                String folderName = entry.getKey();
                List<byte[]> images = entry.getValue();

                int imageIndex = 1;
                for (byte[] imageData : images) {
                    String fileName = folderName + "/image_" + imageIndex + ".png";
                    zipOutputStream.putNextEntry(new ZipEntry(fileName));
                    zipOutputStream.write(imageData);
                    zipOutputStream.closeEntry();
                    imageIndex++;
                }
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

}
