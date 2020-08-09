package com.example.test.flow;

import com.example.flow.ExampleFlow;
import com.example.state.BillOfLadingState;
import com.example.state.LetterOfCreditState;
import com.example.state.PurchaseOrderState;
import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;

abstract class LetterOfCreditTests {
    public MockNetwork network;
    public StartedMockNode buyer;
    public StartedMockNode seller;
    public StartedMockNode issuingBank;
    public StartedMockNode advisingBank;

    public PurchaseOrderState demoPurchaseOrder;
    public LetterOfCreditState demoLetterOfCreditState;
    public BillOfLadingState demoBillOfLadingState;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.example.contract"),
                TestCordapp.findCordapp("com.example.flow"))));
        buyer = network.createPartyNode(new CordaX500Name("Buyer", "Kowloon", "HK"));
        seller = network.createPartyNode(new CordaX500Name("Seller", "Chittagong", "BD"));
        issuingBank = network.createPartyNode(new CordaX500Name("AdvisingBank", "Dhaka", "BD"));
        advisingBank = network.createPartyNode(new CordaX500Name("IssuingBank", "Kowloon", "HK"));
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(buyer, seller, issuingBank, advisingBank)) {
            node.registerInitiatedFlow(ExampleFlow.Acceptor.class);
        }

        demoPurchaseOrder = new PurchaseOrderState(
                "1",
                seller.getInfo().getLegalIdentities().get(0),
                buyer.getInfo().getLegalIdentities().get(0),
                "01-01-2020",
                "product",
                100L,
                5L,
                700L
        );
        demoLetterOfCreditState = new LetterOfCreditState(
                "1",
                "A",
                "31-01-2020",
                demoPurchaseOrder.getSeller(),
                demoPurchaseOrder.getBuyer(),
                advisingBank.getInfo().getLegalIdentities().get(0),
                issuingBank.getInfo().getLegalIdentities().get(0),
                500L,
                "CTG Port",
                "Chittagong",
                "BD",
                "KWL Port",
                "Kowloon",
                "HK",
                demoPurchaseOrder.getProductName(),
                demoPurchaseOrder.getProductQuantity(),
                demoPurchaseOrder.getProductPriceInUSD(),
                demoPurchaseOrder.getProductGrossWeightInKG()
        );
        demoBillOfLadingState = new BillOfLadingState(
                "1",
                demoLetterOfCreditState.getSeller(),
                demoLetterOfCreditState.getBuyer(),
                "CTG Ships Ltd",
                "CTG Ship",
                "02-01-2020",
                "10-01-2020",
                demoLetterOfCreditState.getProductName(),
                "No Damage",
                demoLetterOfCreditState.getProductQuantity(),
                demoLetterOfCreditState.getProductPriceInUSD(),
                demoLetterOfCreditState.getProductGrossWeightInKG(),
                "CTG Port",
                "Chittagong",
                "BD",
                "KWL Port",
                "Kowloon",
                "HK"
        );

        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

}
