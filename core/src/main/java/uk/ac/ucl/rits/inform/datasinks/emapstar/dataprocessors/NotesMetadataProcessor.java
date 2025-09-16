package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.NotesMetadataController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.NotesMetadataMessage;
import uk.ac.ucl.rits.inform.interchange.ResearchOptOut;

import java.time.Instant;

/**
 * Handle processing of notes metadata messages.
 * @author Sarah Keating
 */
@Component
public class NotesMetadataProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonController personController;
    private final VisitController visitController;
    private final NotesMetadataController notesMetadataController;

    /**
     * Notes metadata controller to identify whether metadata needs to be updated; person controller to identify patient.
     * @param notesMetadataController    notes metadata controller
     * @param personController           person controller
     * @param visitController            hospital visit controller
     */
    public NotesMetadataProcessor(NotesMetadataController notesMetadataController,
            PersonController personController, VisitController visitController) {
        this.personController = personController;
        this.visitController = visitController;
        this.notesMetadataController = notesMetadataController;
    }

    /**
     * Process notes metadata message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final NotesMetadataMessage msg, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {

        logger.trace("Processing {}", msg);
        Mrn mrn = getOrCreateMrn(msg, storedFrom);
        HospitalVisit visit = getOrCreateHospitalVisit(msg, mrn, storedFrom);
        notesMetadataController.processMessage(msg, storedFrom);
    }

    /**
     * Update or create patient with research opt out flag set to true.
     * @param msg        research opt out
     * @param storedFrom time that star started processing the message
     * @throws RequiredDataMissingException If MRN and NHS number are both null
     */
    @Transactional
    public void processMessage(ResearchOptOut msg, final Instant storedFrom) throws RequiredDataMissingException {
        personController.updateOrCreateWithResearchOptOut(msg, storedFrom);
    }

    /**
     * Get or create a hospital visit using the visitController.
     * @param msg        Patient condition message
     * @param mrn        MRN
     * @param storedFrom Instant at which the message started being processed
     * @return HospitalVisit
     * @throws RequiredDataMissingException
     */
    private HospitalVisit getOrCreateHospitalVisit(NotesMetadataMessage msg, Mrn mrn, Instant storedFrom)
            throws RequiredDataMissingException {

        HospitalVisit visit = null;

//        if (msg.getVisitNumber().isSave()) {
//            visit = visitController.getOrCreateMinimalHospitalVisit(msg.getVisitNumber().get(), mrn,
//                    msg.getSourceSystem(), msg.getUpdatedDateTime(), storedFrom);
//        }

        return visit;
    }

    /**
     * Get or create an MRN id associated with a patient.
     * @param msg        Patient condition message
     * @param storedFrom Instant at which the message started being processed
     * @return HospitalVisit
     * @throws RequiredDataMissingException
     */
    private Mrn getOrCreateMrn(NotesMetadataMessage msg, Instant storedFrom) throws RequiredDataMissingException {

//        String mrnStr = msg.getMrn();
//        Instant msgUpdatedTime = msg.getUpdatedDateTime();
//
//        return personController.getOrCreateOnMrnOnly(mrnStr, null, msg.getSourceSystem(),
//                msgUpdatedTime, storedFrom);
        return 
    }

}
