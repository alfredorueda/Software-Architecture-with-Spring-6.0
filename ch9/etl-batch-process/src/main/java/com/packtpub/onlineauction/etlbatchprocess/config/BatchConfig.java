package com.packtpub.onlineauction.etlbatchprocess.config;

import com.packtpub.onlineauction.etlbatchprocess.auctions.Auction;
import com.packtpub.onlineauction.etlbatchprocess.auctions.AuctionDto;
import com.packtpub.onlineauction.etlbatchprocess.auctions.AuctionItemReader;
import com.packtpub.onlineauction.etlbatchprocess.bids.Bid;
import com.packtpub.onlineauction.etlbatchprocess.bids.BidDto;
import com.packtpub.onlineauction.etlbatchprocess.bids.BidItemReader;
import com.packtpub.onlineauction.etlbatchprocess.products.Product;
import com.packtpub.onlineauction.etlbatchprocess.products.ProductDto;
import com.packtpub.onlineauction.etlbatchprocess.products.ProductItemReader;
import com.packtpub.onlineauction.etlbatchprocess.users.User;
import com.packtpub.onlineauction.etlbatchprocess.users.UserDto;
import com.packtpub.onlineauction.etlbatchprocess.users.UserItemReader;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final AuctionItemReader auctionItemReader;
    private final ItemProcessor<AuctionDto, Auction> auctionItemProcessor;
    private final ItemWriter<Auction> auctionItemWriter;

    private final BidItemReader bidItemReader;
    private final ItemProcessor<BidDto, Bid> itemProcessor;
    private final ItemWriter<Bid> itemWriter;

    private final UserItemReader userItemReader;
    private final ItemProcessor<UserDto, User> userItemProcessor;
    private final ItemWriter<User> userItemWriter;

    private final ProductItemReader productItemReader;
    private final ItemProcessor<ProductDto, Product> productItemProcessor;
    private final ItemWriter<Product> productItemWriter;

    @Bean
    public Job importBids(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new JobBuilder("importBids", jobRepository)
                .start(importBidsStep(jobRepository, transactionManager))
                .next(importUsersStep(jobRepository, transactionManager))
                .next(importProductsStep(jobRepository, transactionManager))
                .next(importAuctionsStep(jobRepository, transactionManager))
                .next(renameFilesStep(jobRepository, transactionManager, List.of("bidsFile", "usersFile", "productsFile", "auctionsFile")))
                .build();
    }

    @Bean
    public Step importBidsStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("importBidsStep", jobRepository)
                .<BidDto, Bid>chunk(100, transactionManager)
                .reader(bidItemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }

    @Bean
    public Step importUsersStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("importUsersStep", jobRepository)
                .<UserDto, User>chunk(100, transactionManager)
                .reader(userItemReader)
                .processor(userItemProcessor)
                .writer(userItemWriter)
                .build();
    }

    @Bean
    public Step importProductsStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("importProductsStep", jobRepository)
                .<ProductDto, Product>chunk(100, transactionManager)
                .reader(productItemReader)
                .processor(productItemProcessor)
                .writer(productItemWriter)
                .build();
    }

    @Bean
    public Step importAuctionsStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("importAuctionsStep", jobRepository)
                .<AuctionDto, Auction>chunk(100, transactionManager)
                .reader(auctionItemReader)
                .processor(auctionItemProcessor)
                .writer(auctionItemWriter)
                .build();
    }

    @Bean
    public Step renameFilesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, List<String> filePaths) {
        return new StepBuilder("renameFilesStep", jobRepository)
                .tasklet((StepContribution contribution, ChunkContext chunkContext) -> {

                    filePaths.stream().forEach(n -> {

                        String inputFilePath = (String) chunkContext.getStepContext()
                                .getJobParameters()
                                .get(n);

                        File file = new File(inputFilePath);
                        if (file.exists()) {
                            String newFileName = file.getAbsolutePath().replace(".csv", "_processed.csv");
                            Path sourcePath = file.toPath();
                            Path targetPath = new File(newFileName).toPath();

                            try {
//                                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                System.out.println("File renamed to: " + newFileName);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to rename file", e);
                            }
                        } else {
                            System.out.println("File not found: " + inputFilePath);
                        }
                    });
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

}
