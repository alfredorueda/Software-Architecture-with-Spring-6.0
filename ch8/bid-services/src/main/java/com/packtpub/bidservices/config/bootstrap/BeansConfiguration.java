package com.packtpub.bidservices.config.bootstrap;

import com.packtpub.bidservices.adapter.datasources.auction.AuctionDatasource;
import com.packtpub.bidservices.adapter.datasources.auction.AuctionDocumentRepository;
import com.packtpub.bidservices.adapter.datasources.authentication.AuthenticationRestApi;
import com.packtpub.bidservices.adapter.datasources.bid.BidDatasource;
import com.packtpub.bidservices.adapter.datasources.bid.BidDocumentRepository;
import com.packtpub.bidservices.adapter.datasources.bid.BidProducer;
import com.packtpub.bidservices.internal.usecases.CreateAuctionUseCase;
import com.packtpub.bidservices.internal.usecases.CreateBidUseCase;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BeansConfiguration {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public CreateAuctionUseCase createAuctionUseCase(AuctionDocumentRepository auctionDocumentRepository){
        AuctionDatasource auctionDatasource = new AuctionDatasource(auctionDocumentRepository);
        return new CreateAuctionUseCase(auctionDatasource);
    }

//    @Bean
//    public CreateBidUseCase createBidUseCase(BidDocumentRepository bidDocumentRepository, BidProducer bidProducer, BidProducerConfiguration bidProducerConfiguration){
//        BidDatasource bidDatasource = new BidDatasource(bidDocumentRepository, bidProducer, bidProducerConfiguration);
//        return new CreateBidUseCase(bidDatasource);
//    }

    @Bean
    public CreateBidUseCase createBidUseCase(BidDocumentRepository bidDocumentRepository, BidProducer bidProducer){
        BidDatasource bidDatasource = new BidDatasource(bidDocumentRepository, bidProducer);
        return new CreateBidUseCase(bidDatasource);
    }

    @Bean
    public AuthenticationRestApi authenticationRestApi(RestClient restClient, DiscoveryClient discoveryClient){
        return new AuthenticationRestApi(restClient, discoveryClient);
    }

}