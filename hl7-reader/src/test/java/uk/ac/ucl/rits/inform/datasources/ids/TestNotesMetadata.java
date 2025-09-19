package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.NotesMetadataMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test EPIC Patient Notes metadata processing.
 * @author Sarah Keating
 */
@ActiveProfiles("test")
@SpringBootTest
public class TestNotesMetadata extends TestHl7MessageStream {
    private static final String FILE_TEMPLATE = "NotesMetadata/%s.txt";
    private static final String MRN = "40800000";
    private static final Instant CHANGE_TIME = Instant.parse("2013-02-14T09:00:00Z");
    private static final Instant CANCEL_TIME = Instant.parse("2013-02-14T10:00:00Z");
    private static final Instant CLOSED_TIME = Instant.parse("2013-02-14T10:00:00Z");
    private static final Instant REQUEST_TIME = Instant.parse("2013-02-12T11:55:00Z");
    private static final String EPIC = "EPIC";
    private static final String ADVANCED_CARE_CODE = "COD4";
    private static final String VISIT_NUMBER = "123412341234";
    private static final String ADVANCED_DECISION_TYPE_NAME = "FULL ACTIVE TREATMENT";

    /**
     * Read the notes from a tested HL7 message
     * 
     * @param fileName       file name to read
     * @return               a NotesMetadataMessage (an interchange)
     * @throws Exception
     */
    NotesMetadataMessage getNotesMetadata(String fileName) throws Exception {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(String.format(FILE_TEMPLATE, fileName));
        } catch (Exception e) {
            throw e;
        }

        assert msgs != null;
        // filter out any implied ADT messages
        return msgs.stream()
                .filter(msg -> (msg instanceof NotesMetadataMessage))
                .map(o -> (NotesMetadataMessage) o)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Given that nothing has been parsed before
     * When an 
     * @throws Exception shouldn't happen
     */
    @Test
    void testSingleNoteProcessed() throws Exception {
        NotesMetadataMessage notesMetadataMessage = getNotesMetadata("minimal");
        assertEquals(MRN, notesMetadataMessage.getMrn());
        // assertEquals(EPIC, notesMetadataMessage.getSourceSystem());
        // assertEquals(CHANGE_TIME, notesMetadataMessage.getStatusChangeDatetime());
        // assertEquals(REQUEST_TIME, notesMetadataMessage.getRequestedDatetime());
        // assertEquals(ADVANCED_CARE_CODE, notesMetadataMessage.getAdvanceCareCode());
        // assertEquals(VISIT_NUMBER, notesMetadataMessage.getVisitNumber());
        // assertEquals(ADVANCED_DECISION_TYPE_NAME, notesMetadataMessage.getNotesMetadataTypeName());
    }

    /**
     * There shouldn't be multiple TXA segments > 
     */
    @Test
    void testMultipleRequestInMessageThrows() {
      assertThrows(Hl7InconsistencyException.class, () -> getNotesMetadata("multiple_requests"));  
    }
}
