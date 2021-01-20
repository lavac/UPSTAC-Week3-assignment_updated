package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.testrequests.consultation.ConsultationController;
import org.upgrad.upstac.testrequests.consultation.CreateConsultationRequest;
import org.upgrad.upstac.testrequests.consultation.DoctorSuggestion;
import org.upgrad.upstac.testrequests.lab.TestStatus;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class ConsultationControllerTest {
    @Autowired
    ConsultationController consultationController;

    @Autowired
    TestRequestQueryService testRequestQueryService;

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);
        TestRequest testRequest1 = consultationController.assignForConsultation(testRequest.requestId);

        assertEquals(testRequest.requestId, testRequest1.requestId);
        assertEquals(RequestStatus.DIAGNOSIS_IN_PROCESS, testRequest1.getStatus());
        assertNotNull(testRequest1.getConsultation());
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_in_valid_test_request_id_should_throw_exception() {
        Long invalidRequestId = -34L;
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class,
                () -> consultationController.assignForConsultation(invalidRequestId));

        assertTrue(responseStatusException.getMessage().contains("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        createConsultationRequest.setComments("comments");
        createConsultationRequest.setSuggestion(DoctorSuggestion.ADMIT);

        TestRequest testRequest1 = consultationController.updateConsultation(12L, createConsultationRequest);

        assertEquals(testRequest.getRequestId(), testRequest1.getRequestId());
        assertTrue(testRequest1.getStatus().equals(RequestStatus.COMPLETED));
        assertEquals(testRequest.getConsultation().getSuggestion(), testRequest1.getConsultation().getSuggestion());
    }


    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);

        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class,
                () -> consultationController.updateConsultation(-2L, createConsultationRequest));

        assertTrue(responseStatusException.getMessage().contains("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        createConsultationRequest.setSuggestion(null);

        assertThrows(ResponseStatusException.class,
                () -> consultationController.updateConsultation(testRequest.getRequestId(), createConsultationRequest));
    }

    public CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {
        CreateConsultationRequest createConsultationRequest = new CreateConsultationRequest();
        if (testRequest.getLabResult().getResult() == TestStatus.POSITIVE) {
            createConsultationRequest.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
            createConsultationRequest.setComments("you have tested tested positive for Covid 19, Please stay home for 4 weeks");
        } else {
            createConsultationRequest.setSuggestion(DoctorSuggestion.NO_ISSUES);
            createConsultationRequest.setComments("Ok");
        }
        return createConsultationRequest;
    }
}