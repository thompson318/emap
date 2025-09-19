package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
//import ca.uhn.hl7v2.model.v26.message.MDM_T01;
import ca.uhn.hl7v2.model.v26.message.MDM_T02;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import ca.uhn.hl7v2.model.v26.segment.TXA;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.NotesMetadataHl7;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.NotesMetadataMessage;

import java.time.Instant;

/**
 * Factory to generate notes metadata interchange messages from HL7 (i.e. messages with type MDM^T01 or MDM^T08).
 * @author Sarah Keating
 */
@NoArgsConstructor
@Component
public class NotesMetadataFactory {

    /**
     * Build notedata from the message.
     * @param sourceId    message sourceId
     * @param msg         hl7 message
     * @return A single notes allergy representative for one of the IAM segments in the message
     * @throws HL7Exception if message cannot be parsed correctly.
     */
    public NotesMetadataMessage buildNotesMetadata(String sourceId, MDM_T02 msg) throws HL7Exception {

        NotesMetadataMessage notesMetadataMessage = new NotesMetadataMessage();

        MSH msh = (MSH) msg.get("MSH");
        PID pid = (PID) msg.get("PID");
        PV1 pv1 = (PV1) msg.getPV1();
//          EVN evn = (EVN) msg.get("EVN");
        TXA txa = (TXA) msg.getTXA();
 //       Instant recordedDateTime = HL7Utils.interpretLocalTime(evn.getRecordedDateTime());
        NotesMetadataHl7 notesInfo = new NotesMetadataHl7(msh, pid, pv1, txa);

        // generic information
        notesMetadataMessage.setSourceMessageId(sourceId);
        notesMetadataMessage.setSourceSystem(notesInfo.getSendingApplication());
        notesMetadataMessage.setMrn(notesInfo.getMrn());
        notesMetadataMessage.setVisitNumber(notesInfo.getVisitNumberFromPv1orPID());
        notesMetadataMessage.setNoteType(notesInfo.getNotesMetadataTypeString());
        notesMetadataMessage.setStartedDatetime(notesInfo.getNotesMetadataOriginatingInstant());
        notesMetadataMessage.setLastEditDatetime(notesInfo.getNotesMetadataActivityInstant());

        return notesMetadataMessage;
    }

    /**
     * Build notedata from the message.
     * @param sourceId    message sourceId
     * @param msg         hl7 message
     * @return A single notes allergy representative for one of the IAM segments in the message
     * @throws HL7Exception if message cannot be parsed correctly.
     */
    public NotesMetadataMessage addToNotesMetadata(String sourceId, MDM_T02 msg) throws HL7Exception {

        NotesMetadataMessage notesMetadataMessage = new NotesMetadataMessage();

        MSH msh = (MSH) msg.get("MSH");
        PID pid = (PID) msg.get("PID");
        PV1 pv1 = (PV1) msg.getPV1();
        EVN evn = (EVN) msg.get("EVN");
        Instant recordedDateTime = HL7Utils.interpretLocalTime(evn.getRecordedDateTime());

        PatientInfoHl7 notesInfo = new PatientInfoHl7(msh, pid, pv1);
        // generic information
        notesMetadataMessage.setSourceMessageId(sourceId);
        notesMetadataMessage.setSourceSystem(notesInfo.getSendingApplication());
//        notesMetadataMessage.setMrn(notesInfo.getMrn());
//        notesMetadataMessage.setUpdatedDateTime(HL7Utils.interpretLocalTime(evn.getEvn2_RecordedDateTime()));

        return notesMetadataMessage;
    }
}
