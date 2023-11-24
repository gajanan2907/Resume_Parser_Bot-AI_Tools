package com.ResumeParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class ResumeParserSwingApp extends JFrame {

    private final JTextArea textArea;
    private JDialog loaderDialog;

    public ResumeParserSwingApp() {
        setTitle("Upload Resume and Export in Excel");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton uploadButton = new JButton("Upload Resume");

        textArea = new JTextArea(10, 30);
        textArea.setEditable(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(uploadButton);

        setLayout(new BorderLayout());
        add(buttonPanel, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        uploadButton.addActionListener(e -> {
            try {

                uploadButtonActionPerformed();
                showLoader();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                hideLoader();
            }
        });

        centerFrame(this);
    }

    private static void centerFrame(JFrame frame) {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int w = frame.getSize().width;
        int h = frame.getSize().height;
        int x = (dim.width - w) / 4;
        int y = (dim.height - h) / 4;
        frame.setLocation(x, y);
    }

    private void uploadButtonActionPerformed() throws IOException {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Resume Files", "pdf");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            List<File> files = new ArrayList<>();

            for (File file : selectedFiles) {
                textArea.append(file.getName() + "\n");
                files.add(file);
            }
            sendFileToAPI(files);
        }

    }

    private void hideLoader() {
        if (loaderDialog != null) {
            loaderDialog.setVisible(false);
        }
    }

    private void showLoader() {
        loaderDialog = new JDialog(this, "Loading", true);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        JPanel panel = new JPanel();
        panel.add(progressBar);

        loaderDialog.add(panel);
        loaderDialog.setSize(200, 100);
        loaderDialog.setLocationRelativeTo(this);
        loaderDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loaderDialog.setUndecorated(true);
        loaderDialog.setVisible(true);
    }


    private void sendFileToAPI(List<File> files) throws IOException {
        String apiUrl = "https://35ryklyq7xikor77u7hoklhf4y0ppljj.lambda-url.ap-south-1.on.aws/file";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        List<ResumeFileDto> resumeFileDtos = new ArrayList<>();

        for (File file : files) {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("uploaded_Resume", new FileSystemResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            System.out.println("ResponseBody: " + responseEntity.getBody());

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                ApiFileResponse apiFileResponse = objectMapper.readValue(responseEntity.getBody(), ApiFileResponse.class);
                ResumeFileDto resumeFileDto = getResumeFileDto(apiFileResponse);
                resumeFileDtos.add(resumeFileDto);

                System.out.println("API Response: " + apiFileResponse);

            } else {
                System.err.println("API Error Response: " + responseEntity.getBody());
            }
        }
        downloadButtonActionPerformed(resumeFileDtos);

    }

    private static ResumeFileDto getResumeFileDto(ApiFileResponse responseObject) {
        ResumeFileDto resumeFileDto = new ResumeFileDto();
        resumeFileDto.setName(responseObject.getLabel().getPersonDetails().getName());
        resumeFileDto.setEmail(responseObject.getLabel().getPersonDetails().getEmail());
        resumeFileDto.setPhoneNumber(responseObject.getLabel().getPersonDetails().getPhoneNumber());
        resumeFileDto.setExperience(responseObject.getLabel().getPersonDetails().getExperience());
        resumeFileDto.setCity(responseObject.getLabel().getPersonDetails().getCity());
        List<String> designation = new ArrayList<>();
        for (ApiFileResponse.Label.RefinedExperienceDetails experience : responseObject.getLabel().getRefinedExperienceDetails()) {
            designation.add(experience.getDesignation());
        }
        resumeFileDto.setDesignation(designation);

        List<String> skills = new ArrayList<>(responseObject.getLabel().getEmploymentDetails().getSkills());
        resumeFileDto.setSkills(skills);

        return resumeFileDto;
    }

    private void downloadButtonActionPerformed(List<ResumeFileDto> resumeFileDto) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Download Location");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File downloadLocation = fileChooser.getSelectedFile();

            try {

                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Resume Data");

                Row headerRow = sheet.createRow(0);

                headerRow.createCell(0).setCellValue("Candidate Name");
                headerRow.createCell(1).setCellValue("Email id");
                headerRow.createCell(2).setCellValue("Phone Number");
                headerRow.createCell(3).setCellValue("Experience");
                headerRow.createCell(4).setCellValue("Designation");
                headerRow.createCell(5).setCellValue("Candidate Skills");
                headerRow.createCell(6).setCellValue("Current Location");

                int rowNum = 1;
                assert resumeFileDto != null;
                for (ResumeFileDto resume : resumeFileDto) {
                    Row dataRow = sheet.createRow(rowNum++);

                    dataRow.createCell(0).setCellValue(resume.getName());
                    dataRow.createCell(1).setCellValue(resume.getEmail());
                    dataRow.createCell(2).setCellValue(resume.getPhoneNumber());
                    if (resume.getExperience() != null) {
                        dataRow.createCell(3).setCellValue(resume.getExperience());
                    }

                    String resumeDesignation = " ";
                    if (!resume.getDesignation().isEmpty()) {
                        resumeDesignation = resume.getDesignation().get(0);
                    }

                    dataRow.createCell(4).setCellValue(resumeDesignation);

                    String resumeSkills = "";
                    for (String skill : resume.getSkills()) {
                        resumeSkills = resumeSkills.concat(skill + ", ");
                    }
                    dataRow.createCell(5).setCellValue(resumeSkills);
                    dataRow.createCell(6).setCellValue(resume.getCity());
                }

                File outputFile = new File(downloadLocation, "ResumeData.xlsx");
                try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                    workbook.write(fileOut);
                }

                textArea.append("Downloaded Excel file: " + outputFile.getAbsolutePath() + "\n");

            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error downloading data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ResumeParserSwingApp().setVisible(true));
    }
}
