package com.main.controller;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/gemini")
@Slf4j
public class OpenAiController {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @PostMapping(value = "/fetchExcelQueryData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> askQuestion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question
    ) throws IOException {

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        StringBuilder excelData = new StringBuilder();
        for (Row row : sheet) {
            for (Cell cell : row) {
                excelData.append(cell.toString()).append("\t");
            }
            excelData.append("\n");
        }
        workbook.close();

        String prompt = "Here is the Excel data:\n" + excelData + "\n\nQuestion: " + question;

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(openAiApiKey)
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O_MINI)
                .addUserMessage(prompt)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);

        String answer = completion.choices().get(0).message().content().orElse("");

        Map<String, String> response = new HashMap<>();
        response.put("answer", answer);

        return ResponseEntity.ok(response);
    }
}
