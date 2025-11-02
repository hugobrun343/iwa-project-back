package com.iwaproject.announcement.configs;

import com.iwaproject.announcement.entities.CareType;
import com.iwaproject.announcement.repositories.CareTypeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Data seeder that initializes care types on application startup.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    /**
     * Logger instance.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DataSeeder.class);

    /**
     * Care type repository.
     */
    private final CareTypeRepository careTypeRepository;

    /**
     * Run the seeder on application startup.
     *
     * @param args command line arguments
     */
    @Override
    public void run(final String... args) {
        seedCareTypes();
    }

    /**
     * Seed care types if the table is empty.
     */
    private void seedCareTypes() {
        long count = careTypeRepository.count();

        if (count == 0) {
            LOGGER.info("Seeding care types...");

            List<String> careTypeLabels = Arrays.asList(
                "Home Care",
                "Medical Care",
                "Companionship",
                "Meal Preparation",
                "Transportation",
                "Housekeeping",
                "Personal Care",
                "Medication Management",
                "Physical Therapy",
                "Nursing Care"
            );

            List<CareType> careTypes = careTypeLabels.stream()
                .map(label -> {
                    CareType careType = new CareType();
                    careType.setId(null);
                    careType.setLabel(label);
                    return careType;
                })
                .toList();

            careTypeRepository.saveAll(careTypes);

            LOGGER.info("✅ Successfully seeded {} care types",
                    careTypes.size());
        } else {
            LOGGER.info(
                    "Care types table already contains {} entries,"
                            + " skipping seeding", count);
        }
    }
}
