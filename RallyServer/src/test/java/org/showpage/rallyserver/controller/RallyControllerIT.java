package org.showpage.rallyserver.controller;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.showpage.rallyserver.IntegrationTest;

/**
 * Integration tests for RallyController.
 * Tests are run in the order they appear in the file.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RallyControllerIT extends IntegrationTest {

    @Test
    @Order(1)
    public void testGetAllRallies() throws Exception {
        // Get all rallies as the organizer
        RR_PageUiRally response = get_ForRM("/api/rallies", tr_PageUiRally);

        // Check that the response is valid
        check(response);

        System.out.println("Successfully retrieved rallies. Total: " +
            (response.getData() != null ? response.getData().getTotalElements() : 0));
    }
}
