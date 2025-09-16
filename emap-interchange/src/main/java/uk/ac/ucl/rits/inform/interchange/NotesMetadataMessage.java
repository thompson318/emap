package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Interchange format of a MetadataNotes message.
 * @author Sarah Keating
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class NotesMetadataMessage extends EmapOperationMessage implements Serializable {

    /**
     * Notes with metadata attached.
     */
    private List<String> notes = new ArrayList<>();

    /**
     * Adds a list of strings to the notes.
     * @param notesMetadata Collection of strings, each representing an allergy reaction.
     */
    public void addAllReactions(Collection<String> notesMetadata) {
        notes.addAll(notesMetadata);
    }

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
