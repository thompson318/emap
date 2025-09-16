package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import ca.uhn.hl7v2.model.v26.segment.IAM;
import ca.uhn.hl7v2.model.v26.message.ADT_A60;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.ConditionAction;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
//import uk.ac.ucl.rits.inform.interchange.NotesMetadata;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Factory to generate notes metadata interchange messages from HL7 (i.e. messages with type MDM^T01 or MDM^T08).
 * @author Sarah Keating
 */
@NoArgsConstructor
@Component
public class NotesMetadataFactory {

    /**
     * Build notes metadata from MDM HL7 message.
     * @param sourceId message sourceId
     * @param msg      hl7 message
     * @return list of notes metadata
     * @throws HL7Exception if message cannot be parsed correctly
     */
    public Collection<NotesMetadata> buildNotesMetadata(String sourceId, ADT_A60 msg) throws HL7Exception {
        MSH msh = msg.getMSH();
        PID pid = msg.getPID();
        PV1 pv1 = msg.getPV1();
        EVN evn = msg.getEVN();
        PatientInfoHl7 patientInfo = new PatientInfoHl7(msh, pid, pv1);
        int reps = msg.getIAMReps();
        Collection<NotesMetadata> notes_metadata = new ArrayList<>(reps);
        for (int i = 0; i < reps; i++) {
            IAM allergySegment = msg.getIAM(i);
            NotesMetadata notesMetadata = buildNotesMetadata(sourceId, evn, patientInfo, allergySegment);
            allergies.add(notesMetadata);
        }
        return allergies;
    }

    /**
     * Build patient allergy from one of the IAM segments of the message.
     * @param sourceId    message sourceId
     * @param evn         segment of the message relating to the specific trigger event of the message
     * @param patientInfo segment of the message relating to patient information
     * @param iam         hl7 message
     * @return A single patient allergy representative for one of the IAM segments in the message
     * @throws HL7Exception if message cannot be parsed correctly.
     */
    private NotesMetadata buildNotesMetadata(String sourceId, EVN evn, PatientInfoHl7 patientInfo, IAM iam) throws HL7Exception {

        NotesMetadata notesMetadata = new NotesMetadata();

        // generic information
        notesMetadata.setSourceMessageId(sourceId);
        notesMetadata.setSourceSystem(patientInfo.getSendingApplication());
        notesMetadata.setMrn(patientInfo.getMrn());
        notesMetadata.setUpdatedDateTime(HL7Utils.interpretLocalTime(evn.getEvn2_RecordedDateTime()));

        var allergyAction = iam.getIam6_AllergyActionCode().getCne1_Identifier().getValueOrEmpty();
        switch (allergyAction) {
            case "A":
                notesMetadata.setAction(ConditionAction.ADD);
                break;
            case "D":
                notesMetadata.setAction(ConditionAction.DELETE);
                break;
            case "U":  // Is an explicit "update"
            case "X":  // or a "no change"
                notesMetadata.setAction(ConditionAction.UPDATE);
                break;
            default:
                throw new HL7Exception("Failed to set the action from " + allergyAction);
        }

        // allergy specific information
        var allergyStatus = iam.getAllergyClinicalStatusCode().getCwe1_Identifier().getValueOrEmpty();
        notesMetadata.setStatus(InterchangeValue.buildFromHl7(allergyStatus));
        var allergyId = iam.getIam7_AllergyUniqueIdentifier().getEntityIdentifier().getValueOrEmpty();
        notesMetadata.setEpicConditionId(InterchangeValue.buildFromHl7(Long.valueOf(allergyId)));
        notesMetadata.setOnsetDate(InterchangeValue.buildFromHl7(HL7Utils.interpretDate(iam.getIam11_OnsetDate())));
        notesMetadata.setSubType(InterchangeValue.buildFromHl7(iam.getAllergenTypeCode().getCwe1_Identifier().getValueOrEmpty()));
        var allergyCode = iam.getAllergenCodeMnemonicDescription().getText().getValueOrEmpty();
        notesMetadata.setConditionCode(allergyCode);
        notesMetadata.setConditionName(InterchangeValue.buildFromHl7(allergyCode));
        notesMetadata.setAddedDatetime(HL7Utils.interpretLocalTime(iam.getReportedDateTime()));
        notesMetadata.setSeverity(InterchangeValue.buildFromHl7(iam.getAllergySeverityCode().getCwe1_Identifier().getValue()));

        // add reactions of which there can be multiple
        for (ST reactionCode : iam.getIam5_AllergyReactionCode()) {
            if (!reactionCode.isEmpty()) {
                notesMetadata.getReactions().add(reactionCode.getValue());
            }
        }
        return notesMetadata;
    }
}
