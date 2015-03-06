package com.trifork.conference.leadscanner;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Address;
import ezvcard.property.SimpleProperty;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

@Configuration
@EnableBatchProcessing
@SpringBootApplication
public class BatchConfiguration implements CommandLineRunner {
    @Value("${in:-}")
    String inFilePath;

    @Bean
    public FlatFileItemReader<Scan> flatFileItemReader() {
        FlatFileItemReader<Scan> reader = new FlatFileItemReader<>();
        reader.setRecordSeparatorPolicy(new DefaultRecordSeparatorPolicy());
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper());
        reader.setResource(inFilePath.trim().equals("-") ? new InputStreamResource(System.in) : new FileSystemResource(inFilePath));
        return reader;
    }

    @Bean
    public DefaultLineMapper<Scan> lineMapper() {
        DefaultLineMapper<Scan> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(lineTokenizer());
        lineMapper.setFieldSetMapper(fieldSetMapper());
        return lineMapper;
    }

    @Bean
    public BeanWrapperFieldSetMapper<Scan> fieldSetMapper() {
        BeanWrapperFieldSetMapper<Scan> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Scan.class);
        return mapper;
    }

    @Bean
    public DelimitedLineTokenizer lineTokenizer() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer(",");
        lineTokenizer.setNames(new String[]{"scanned", "title", "raw_text", "barcode_type"});
        lineTokenizer.setQuoteCharacter('"');
        return lineTokenizer;
    }

    @Bean
    public ItemProcessor<Scan, List<String>> ScanToVcardItemProcessor() {
        return item -> {
            VCard vcard = Ezvcard.parse(item.getRawText()).first();

            if (vcard == null) {
                return null;
            }
            return asList(
                    getValueOf(vcard.getFormattedName()), //Name
                    getFirstValueOf(vcard.getEmails()), //Email
                    getFirstValueOf(vcard.getTitles()),
                    getAddressOf(vcard.getAddresses())
            );
        };
    }

    private String getAddressOf(List<Address> addresses) {
        return addresses.stream().findFirst().map(address -> asList(
                address.getStreetAddress(),
                address.getLocality(),
                address.getPostalCode(),
                address.getCountry()
        ).stream().map(StringUtils::defaultString).collect(joining(","))).orElse(",,,");
    }

    private static <T> T getValueOf(SimpleProperty<T> property) {
        if (property != null) {
            return property.getValue();
        }
        return null;
    }

    private static <T> T getFirstValueOf(List<? extends SimpleProperty<T>> propertyList) {
        if (propertyList != null && propertyList.size() > 0) {
            return getValueOf(propertyList.get(0));
        }
        return null;
    }


    @Bean
    public ItemWriter<List<String>> itemWriter() {
        return items -> items.stream().map(fields -> fields.stream().collect(joining(";"))).forEach(System.out::println);
    }

    @Bean
    public Job importLeadScansJob(JobBuilderFactory jobs, Step step1) {
        return jobs.get("importLeadScansJob")
                .incrementer(new RunIdIncrementer())
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public Step step1(StepBuilderFactory stepBuilderFactory, ItemReader<Scan> flatFileItemReader, ItemProcessor<Scan, List<String>> scanToVcardItemProcessor, ItemWriter<List<String>> itemWriter) {
        return stepBuilderFactory.get("step1")
                .<Scan, List<String>>chunk(10)
                .reader(flatFileItemReader)
                .processor(scanToVcardItemProcessor)
                .writer(itemWriter)
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(BatchConfiguration.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        System.out.println("strings = " + strings);
    }
}
