package com.example.yes4all.review;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

@EnableScheduling
@SpringBootApplication
@Slf4j
public class Yes4allReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(Yes4allReviewApplication.class, args);
    }

    @Component
    @RequiredArgsConstructor
    class ReviewScheduler {

        private final JavaMailSender emailSender;

        private static final String FILE_PATH = "review_data.txt";

        @Scheduled(fixedRate = 60000)
        public void fetchAndProcessHtmlContent() {
            try {
                log.info("Processing to check review");
                RestTemplate restTemplate = new RestTemplate();
                String htmlContent = restTemplate.getForObject("https://reviewscongty.me/company/cong-ty-tnhh-dich-vu-thuong-mai-yesall?sort_by=latest", String.class);
                String processedHtml = processHtmlContent(htmlContent);
                if (isNewReview(processedHtml)) {
                    log.info("New review found");
                    sendEmail(processedHtml);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String processHtmlContent(String html) {
            String returnData = html.replaceAll("> <span ", "><span ")
                    .replaceAll("Xem thêm", "")
                    .replaceAll("\\.\\.\\.", "")
                    .replaceAll("_view-more-btn", "")
                    .replaceAll("_view-more-text", "")
                    .replaceAll("_view-more-content", "")
                    .replaceAll("this.onerror = null; this.src=window.WEB.noImage", "")
                    .replaceAll("https://reviewscongty.me/storage/app/uploads/public/companies/cong-ty-tnhh-dich-vu-thuong-mai-yesall.png?t=1704357127", "")
                    .replaceAll("/storage/app/uploads/public/companies/cong-ty-tnhh-dich-vu-thuong-mai-yesall.png?t=1704357127", "")
                    .replaceAll("https://reviewscongty.me/themes/ocean/assets/images/default-avatar.png", "")
                    .replaceAll("https://reviewscongty.me/themes/ocean/assets/images/logo-web.png", "");

            log.info("Processed HTML");
            return returnData;
        }

        private boolean isNewReview(String html) {
            Path path = Paths.get(FILE_PATH);
            try {
                if (Files.exists(path)) {
                    String content = new String(Files.readAllBytes(path));
                    String reviewCurrent = getReviewFileData(html);
                    return !content.equals(reviewCurrent);
                } else {
                    Files.createFile(path);
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        private void saveToLocalStorage(String content) {
            try {
                FileWriter fileWriter = new FileWriter(FILE_PATH);
                fileWriter.write(content);
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendEmail(String htmlContent) throws MessagingException, UnsupportedEncodingException {
            String emailTemplate = extractReviewBox(htmlContent);
            MimeMessage message = emailSender.createMimeMessage();
            message.setSubject("Có review mới của Yes4All");
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(new InternetAddress("reviewyes4all@review.com", "Review Yes4All"));
//            helper.setTo("voducminh140201@gmail.com");
            helper.setTo("hoangngoc14201@gmail.com");
            helper.setCc("thaithuyngan1308@gmail.com");
            helper.setSubject("Có review mới của Yes4All");
            helper.setText(emailTemplate, true);

            emailSender.send(message);
        }

        private String getReviewFileData(String html) {
            Document doc = Jsoup.parse(html);
            Element totalReview = doc.selectFirst(".mb-0.text-14.text-dark b");

            return totalReview.outerHtml();
        }

        private String extractReviewBox(String html) {
            Document doc = Jsoup.parse(html);
            Element reviewBox = doc.selectFirst(".comment-content.box-text-content");

            StringBuilder htmlWithEmail = new StringBuilder();

            htmlWithEmail.append("<div style=color:#000><p>Hi all,<p>Vừa có review mới của Yes4All, xem ngay <a href=\"https://reviewscongty.me/company/cong-ty-tnhh-dich-vu-thuong-mai-yesall?sort_by=latest\">tại đây</a></div>");

            htmlWithEmail.append("<div style=\"background: #fff;")
                    .append("border-radius: 5px;")
                    .append("display: inline-block;")
                    .append("margin: 1rem;")
                    .append("position: relative;")
                    .append("padding: 20px;")
                    .append("box-shadow: 0 19px 38px rgba(0,0,0,0.30), 0 15px 12px rgba(0,0,0,0.22);\">");

            if (reviewBox != null) {
                htmlWithEmail.append(reviewBox.outerHtml());
            }

            saveToLocalStorage(getReviewFileData(html));

            htmlWithEmail.append("</div>");
            htmlWithEmail.append("<p style=color:#000>Best wishes</p>");

            return htmlWithEmail.toString();
        }
    }

    @RestController
    class ReviewController {

        private static final String FILE_PATH = "review_data.txt";

        @GetMapping("/review")
        public String getReviewData() {
           return "Hello";
        }
    }
}
