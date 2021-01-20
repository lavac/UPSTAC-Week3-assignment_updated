package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabRequestController;

import java.util.Objects;


@SpringBootTest
@Slf4j
class LabRequestControllerTest {
    @Autowired
    LabRequestController labRequestController;

    @Autowired
    TestRequestQueryService testRequestQueryService;

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_update_the_request_status(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.INITIATED);

        TestRequest assignedTestRequest = labRequestController.assignForLabTest(testRequest.getRequestId());

        Assertions.assertEquals(testRequest.getRequestId(), assignedTestRequest.getRequestId());
        Assertions.assertEquals(RequestStatus.LAB_TEST_IN_PROGRESS, assignedTestRequest.getStatus());
        Assertions.assertNotNull(assignedTestRequest.getLabResult());
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_throw_exception(){
        Long invalidRequestId= -34L;
        ResponseStatusException responseStatusException = Assertions.assertThrows(ResponseStatusException.class,
                () -> labRequestController.assignForLabTest(invalidRequestId));
        Assertions.assertTrue(Objects.requireNonNull(responseStatusException.getMessage()).contains("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_valid_test_request_id_should_update_the_request_status_and_update_test_request_details(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);

        TestRequest testRequest1 = labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);

        Assertions.assertEquals(testRequest1.getRequestId(), testRequest.getRequestId());
        Assertions.assertEquals(RequestStatus.COMPLETED, testRequest1.getStatus());
        Assertions.assertEquals(testRequest1.getLabResult(), testRequest.getLabResult());
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_test_request_id_should_throw_exception(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);

        CreateLabResult createLabResult = getCreateLabResult(testRequest);

        ResponseStatusException responseStatusException = Assertions.assertThrows(ResponseStatusException.class,
                () -> {
                    Long requestId = testRequest.getRequestId();
                    labRequestController.updateLabTest(requestId, createLabResult);
                });

        Objects.requireNonNull(responseStatusException.getMessage());
        Assertions.assertTrue(responseStatusException.getMessage().contains("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_empty_status_should_throw_exception(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);

        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        createLabResult.setResult(null);

        ResponseStatusException responseStatusException = Assertions.assertThrows(ResponseStatusException.class,
                () -> labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult));

        Assertions.assertTrue(Objects.requireNonNull(responseStatusException.getMessage()).contains("ConstraintViolationException"));
    }

    public CreateLabResult getCreateLabResult(TestRequest testRequest) {

        CreateLabResult createLabResult = new CreateLabResult();
        createLabResult.setResult(testRequest.labResult.getResult());

        return createLabResult;
    }
}