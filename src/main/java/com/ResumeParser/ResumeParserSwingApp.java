package com.ResumeParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class ResumeParserSwingApp extends JFrame {

  private final JTextArea textArea;
  private JDialog loaderDialog;
  private JProgressBar progressBar;
  private Image image =
      Toolkit.getDefaultToolkit().getImage("src/main/resources/ResumeParser1.png");

  public ResumeParserSwingApp() {
    setIconImage(image);
    setTitle("Resume Parser - AI Tools");
    setSize(800, 600);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JButton uploadButton = new JButton("Select Resumes");
    customizeButton(uploadButton);

    textArea = new JTextArea(20, 50);
    textArea.setEditable(false);
    JScrollPane scrollPane = new JScrollPane(textArea);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add(uploadButton);

    setLayout(new BorderLayout());
    add(buttonPanel, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);

    uploadButton.addActionListener(
        e -> {
          uploadButtonActionPerformed();
        });

    centerFrame(this);
  }

  private void customizeButton(JButton button) {
    button.setBackground(new Color(63, 156, 255));
    button.setForeground(Color.WHITE);
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
  }

  private void centerFrame(JFrame frame) {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int frameWidth = frame.getSize().width;
    int frameHeight = frame.getSize().height;

    int x = (screenSize.width - frameWidth) / 2;
    int y = (screenSize.height - frameHeight) / 2;

    frame.setLocation(x, y);
  }

  private JDialog createLoadingDialog(Frame parent) {
    JDialog dialog = new JDialog(parent, "Loading...", true);
    JPanel panel = new JPanel(new BorderLayout());

    progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);

    progressBar.setForeground(new Color(63, 156, 255));

    JLabel label = new JLabel("Please wait. Your data is loading...");
    label.setHorizontalAlignment(JLabel.CENTER);

    label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
    panel.add(progressBar, BorderLayout.CENTER);
    panel.add(label, BorderLayout.SOUTH);

    dialog.getContentPane().add(panel);
    dialog.setSize(300, 45);
    dialog.setLocationRelativeTo(parent);
    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    return dialog;
  }

  private void hideLoader() {
    if (loaderDialog != null) {
      loaderDialog.setVisible(false);
    }
  }

  private void showLoader() {
    loaderDialog = createLoadingDialog(this);
    loaderDialog.setUndecorated(true);
    loaderDialog.setVisible(true);
  }

  private void uploadButtonActionPerformed() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setMultiSelectionEnabled(true);
    FileNameExtensionFilter filter = new FileNameExtensionFilter(".pdf", "pdf");
    fileChooser.setFileFilter(filter);

    int result = fileChooser.showOpenDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
      File[] selectedFiles = fileChooser.getSelectedFiles();
      List<ResumeFileDto> resumeInfo = new ArrayList<>();

      if (selectedFiles.length > 0) {
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
          @Override
          protected Void doInBackground() {
            int totalFiles = selectedFiles.length;
            int batchSize = 100;
            for (int i = 0; i < totalFiles; i += batchSize) {
              int endIndex = Math.min(i + batchSize, totalFiles);
              List<File> batchFiles = Arrays.asList(Arrays.copyOfRange(selectedFiles, i, endIndex));
              resumeInfo.addAll(sendFileToAPI(batchFiles));
                publish(endIndex);
            }
            downloadButtonActionPerformed(resumeInfo);
            return null;
          }

          @Override
          protected void done() {
            hideLoader();
            loaderDialog.dispose();
          }
        };

        worker.execute();
        showLoader();
      } else {
        showError("Please select files for export.");
      }
    } else {
      showError("Export operation canceled or no files selected.");
    }
  }

  private void showMessage(String message) {
    textArea.append(message + "\n");
  }

  private void showError(String errorMessage) {
    JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
  }

  private void showSuccessMessage(String message) {
    JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
  }

  private List<ResumeFileDto> sendFileToAPI(List<File> files) {
    String apiUrl = "https://sswbyclmkr67igcy6iiae7vjxq0wnmlh.lambda-url.ap-south-1.on.aws/file";
    CountDownLatch latch = new CountDownLatch(files.size());
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    List<ResumeFileDto> resumeFileDtos = Collections.synchronizedList(new ArrayList<>());

    ExecutorService executorService = Executors.newFixedThreadPool(Math.min(files.size(), 100));

    for (File file : files) {
      executorService.submit(() -> {
        try {
          MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
          body.add("uploaded_Resume", new FileSystemResource(file));

          HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

          ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

          if (responseEntity.getStatusCode().is2xxSuccessful()) {
            ObjectMapper objectMapper = new ObjectMapper();
            ApiFileResponse apiFileResponse = objectMapper.readValue(responseEntity.getBody(), ApiFileResponse.class);
            ResumeFileDto resumeFileDto = getResumeFileDto(apiFileResponse, file);
            resumeFileDtos.add(resumeFileDto);
          } else {
            System.err.println("API Error Response: " + responseEntity.getBody());
          }
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          latch.countDown();
        }
      });
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      executorService.shutdown();
    }

    return resumeFileDtos;
  }

  private static ResumeFileDto getResumeFileDto(ApiFileResponse responseObject, File file) {
    ResumeFileDto resumeFileDto = new ResumeFileDto();
    resumeFileDto.setName(responseObject.getLabel().getPersonDetails().getName());
    resumeFileDto.setFileName(file.getName());
    resumeFileDto.setEmail(responseObject.getLabel().getPersonDetails().getEmail());
    resumeFileDto.setPhoneNumber(responseObject.getLabel().getPersonDetails().getPhoneNumber());
    resumeFileDto.setExperience(responseObject.getLabel().getPersonDetails().getExperience());
    resumeFileDto.setCity(responseObject.getLabel().getPersonDetails().getCity());
    List<String> designation = new ArrayList<>();
    for (ApiFileResponse.Label.RefinedExperienceDetails experience :
        responseObject.getLabel().getRefinedExperienceDetails()) {
      designation.add(experience.getDesignation());
    }
    resumeFileDto.setDesignation(designation);

    List<String> skills =
        new ArrayList<>(responseObject.getLabel().getEmploymentDetails().getSkills());
    resumeFileDto.setSkills(skills);

    return resumeFileDto;
  }

  private void downloadButtonActionPerformed(List<ResumeFileDto> resumeFileDto) {
    JFileChooser fileChooser = new JFileChooser();
    FileNameExtensionFilter filter = new FileNameExtensionFilter(".xlsx", "xlsx");
    fileChooser.setFileFilter(filter);
    fileChooser.setDialogTitle("Save as excel");
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    int result = fileChooser.showSaveDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
      File downloadLocation = fileChooser.getSelectedFile();

      String fileNameWithExtension = downloadLocation.getAbsolutePath();
      if (!fileNameWithExtension.toLowerCase().endsWith(".xlsx")) {
        fileNameWithExtension += ".xlsx";
      }

      File outputFile = new File(fileNameWithExtension);

      if (outputFile.exists()) {
        int overwriteConfirmation = JOptionPane.showConfirmDialog(
                this,
                "The file already exists. Do you want to overwrite it?",
                "File Exists",
                JOptionPane.YES_NO_OPTION);

        if (overwriteConfirmation != JOptionPane.YES_OPTION) {
          showMessage("Export operation canceled.");
          return;
        }
      }

      try {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Resume Data");

        Row headerRow = sheet.createRow(0);

        headerRow.createCell(0).setCellValue("FileName");
        headerRow.createCell(1).setCellValue("Candidate Name");
        headerRow.createCell(2).setCellValue("Email id");
        headerRow.createCell(3).setCellValue("Phone Number");
        headerRow.createCell(4).setCellValue("Experience");
        headerRow.createCell(5).setCellValue("Designation");
        headerRow.createCell(6).setCellValue("Candidate Skills");
        headerRow.createCell(7).setCellValue("Current Location");

        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
          Cell cell = headerRow.getCell(i);
          cell.setCellStyle(boldStyle);
        }

        int rowNum = 1;
        assert resumeFileDto != null;
        for (ResumeFileDto resume : resumeFileDto) {
          Row dataRow = sheet.createRow(rowNum++);
          dataRow.createCell(0).setCellValue(resume.getFileName());
          dataRow.createCell(1).setCellValue(resume.getName());
          dataRow.createCell(2).setCellValue(resume.getEmail());
          dataRow.createCell(3).setCellValue(resume.getPhoneNumber());
          if (resume.getExperience() != null) {
            dataRow.createCell(4).setCellValue(resume.getExperience());
          }

          String resumeDesignation = " ";
          if (!resume.getDesignation().isEmpty()) {
            resumeDesignation = resume.getDesignation().get(0);
          }

          dataRow.createCell(5).setCellValue(resumeDesignation);

          String resumeSkills = "";
          for (String skill : resume.getSkills()) {
            resumeSkills = resumeSkills.concat(skill + ", ");
          }
          dataRow.createCell(6).setCellValue(resumeSkills);
          dataRow.createCell(7).setCellValue(resume.getCity());
        }

        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
          workbook.write(fileOut);
        }
        showSuccessMessage("Resume exported successfully.! Please Check..");
        showMessage("Downloaded Excel file: " + outputFile.getName());

      } catch (IOException e) {
        e.printStackTrace();
        showError("Error downloading data: " + e.getMessage());
      }
    }
  }


  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new ResumeParserSwingApp().setVisible(true));
  }
}
