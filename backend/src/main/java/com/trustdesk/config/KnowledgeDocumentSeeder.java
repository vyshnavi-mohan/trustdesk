package com.trustdesk.config;

import com.trustdesk.knowledge.KnowledgeDocument;
import com.trustdesk.knowledge.KnowledgeDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class KnowledgeDocumentSeeder implements ApplicationRunner {

    private final KnowledgeDocumentRepository repository;

    @Value("${trustdesk.data.directory}")
    private String dataDirectory;

    private static final Pattern DOC_ID_PATTERN =
            Pattern.compile("(?m)^Doc ID:\\s*(\\S+)\\s*$");

    private static final Map<String, String> CATEGORIES = Map.of(
            "account_security_policy", "account_security",
            "adversarial_vendor_note", "adversarial",
            "billing_policy", "billing",
            "coupon_policy", "coupon",
            "refund_policy", "refund",
            "shipping_policy", "shipping",
            "support_security_playbook", "security",
            "warranty_policy", "warranty"
    );

    public KnowledgeDocumentSeeder(
            KnowledgeDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {

        Path directory = Path.of(dataDirectory, "knowledge_base");

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> path.toString().endsWith(".md"))
                    .forEach(this::loadDocument);
        }

        System.out.println("Knowledge Base loading completed.");
    }

    private void loadDocument(Path file) {

        try {
            String content = Files.readString(file);

            Matcher matcher = DOC_ID_PATTERN.matcher(content);

            if (!matcher.find()) {
                throw new IllegalStateException(
                        "Missing Doc ID in file: " + file);
            }

            String documentId = matcher.group(1);

            if (repository.existsById(documentId)) {
                return;
            }

            String title = content.lines()
                    .filter(line -> line.startsWith("# "))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing title in file: " + file))
                    .substring(2).trim();

            String fileName = file.getFileName()
                    .toString()
                    .replace(".md", "");

            String category = CATEGORIES.get(fileName);

            if (category == null) {
                throw new IllegalStateException(
                        "Unknown Knowledge Base category: " + fileName);
            }

            KnowledgeDocument document = new KnowledgeDocument(
                    documentId,
                    title,
                    category,
                    content,
                    file.getFileName().toString()
            );

            repository.save(document);

            System.out.println(
                    "Loaded Knowledge Document: " + documentId);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to load Knowledge Base file: " + file,
                    exception);
        }
    }
}
