package com.db.tflm.sdpingestion.simulation;

import com.db.tflm.sdpingestion.SpringApplicationContext;
import com.db.tflm.sdpingestion.util.PartyUtil;
import com.db.tflm.sdpingestion.util.RequestUtil;
import com.db.tflm.sdpingestion.util.TransactionUtil;
import com.db.tradesense.kannon.InstrumentEventType;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static com.db.tflm.sdpingestion.simulation.ScenarioConstants.*;

public class LoanSimulation extends Simulation {
    
    // Constants
    private static final String SCENARIO_NAME = "Loan SDP Simulation";
    private static final String PARTY_LOAN_ID = "8590";
    private static final String LOAN_BOOKING_INITIATED_GROUP = "Loan BOOKING_INITIATED group";
    private static final String LOAN_BOOKING_SUCCESSFUL_GROUP = "Loan BOOKING_SUCCESSFUL group";
    private static final String LOAN_BOOKING_CANCELLED_GROUP = "Loan BOOKING_CANCELLED group";
    private static final String LOAN_EXTENSION_REQUESTED_GROUP = "Loan EXTENSION_REQUESTED group";
    private static final String LOAN_SETTLED_PARTIALLY_GROUP = "Loan SETTLED_PARTIALLY group";
    
    private static final String LOAN_TEMPLATE = "performance/loan.json";
    
    // Setup and teardown methods
    @Override
    public void before() {
        SpringApplicationContext.getInstance();
        initParties();
    }
    
    @Override
    public void after() {
        SpringApplicationContext.getInstance().close();
    }
    
    /**
     * Initialize parties required for the simulation
     */
    private void initParties() {
        PartyUtil.addParty("cRDS", PARTY_LOAN_ID, PARTY_LOAN_ID, true);
    }
    
    /**
     * Creates a chain builder for sending requests based on event type
     */
    private ChainBuilder sendRequests(InstrumentEventType eventType, int repeatNum, String filePath) {
        return RequestUtil.sendInstrumentToKafkaAndReceiveInInternalQueue(
            eventType,
            repeatNum,
            filePath,
            instrument -> {
                if (!InstrumentEventType.BOOKING_INITIATED.equals(instrument.getEventType())) {
                    TransactionUtil.addLoanTransaction(
                        instrument.getPosition().getPayload().getPosition().getPositionQuantity(),
                        instrument.getPosition().getPayload().getPosition().getCurrency().getId(),
                        instrument.getLoan().getPayload()
                    );
                }
            }
        );
    }
    
    /**
     * Overloaded method with default parameters
     */
    private ChainBuilder sendRequests(InstrumentEventType eventType) {
        return sendRequests(eventType, NUM_TRANSACTIONS, LOAN_TEMPLATE);
    }
    
    /**
     * Scenario definition
     */
    private ScenarioBuilder scn = scenario(SCENARIO_NAME)
        .group("WARMUP").on(
            exec(sendRequests(InstrumentEventType.BOOKING_INITIATED, WARMUP_NUM_TRANSACTIONS, LOAN_TEMPLATE))
        )
        .group(LOAN_BOOKING_INITIATED_GROUP).on(
            exec(sendRequests(InstrumentEventType.BOOKING_INITIATED))
        )
        .group(LOAN_BOOKING_SUCCESSFUL_GROUP).on(
            exec(sendRequests(InstrumentEventType.BOOKING_SUCCESSFUL))
        )
        .group(LOAN_BOOKING_CANCELLED_GROUP).on(
            exec(sendRequests(InstrumentEventType.BOOKING_CANCELLED))
        )
        .group(LOAN_EXTENSION_REQUESTED_GROUP).on(
            exec(sendRequests(InstrumentEventType.EXTENSION_REQUESTED))
        )
        .group(LOAN_SETTLED_PARTIALLY_GROUP).on(
            exec(sendRequests(InstrumentEventType.SETTLED_PARTIALLY))
        );
    
    // Test setup
    {
        setUp(scn.injectOpen(atOnceUsers(1)))
        .assertions(
            global().successfulRequests().percent().is(SUCCESSFUL_REQUESTS_PERCENTAGE),
            
            // BOOKING_INITIATED assertions
            details(LOAN_BOOKING_INITIATED_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().max().lt(MAX_RESPONSE_TIME),
            details(LOAN_BOOKING_INITIATED_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().mean().lt(MEAN_RESPONSE_TIME),
            
            // BOOKING_SUCCESSFUL assertions
            details(LOAN_BOOKING_SUCCESSFUL_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().max().lt(MAX_RESPONSE_TIME),
            details(LOAN_BOOKING_SUCCESSFUL_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().mean().lt(MEAN_RESPONSE_TIME),
            
            // BOOKING_CANCELLED assertions
            details(LOAN_BOOKING_CANCELLED_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().max().lt(MAX_RESPONSE_TIME),
            details(LOAN_BOOKING_CANCELLED_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().mean().lt(MEAN_RESPONSE_TIME),
            
            // EXTENSION_REQUESTED assertions
            details(LOAN_EXTENSION_REQUESTED_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().max().lt(MAX_RESPONSE_TIME),
            details(LOAN_EXTENSION_REQUESTED_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().mean().lt(MEAN_RESPONSE_TIME),
            
            // SETTLED_PARTIALLY assertions
            details(LOAN_SETTLED_PARTIALLY_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().max().lt(MAX_RESPONSE_TIME),
            details(LOAN_SETTLED_PARTIALLY_GROUP + " / " + SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS)
                .responseTime().mean().lt(MEAN_RESPONSE_TIME)
        );
    }
}