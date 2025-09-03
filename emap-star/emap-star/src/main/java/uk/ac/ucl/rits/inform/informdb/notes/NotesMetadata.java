package uk.ac.ucl.rits.inform.informdb.notes;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

/**
 * \brief Tracks the notes recorded on a patience record.
 * <p>
 * This doesn't record the actual note
 * @author Sarah Keating
 */
@Entity
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@Table(indexes = {
        @Index(name = "pm_location", columnList = "locationId"),
        @Index(name = "pm_hospital_visit", columnList = "hospitalVisitId"),
        @Index(name = "pm_note_type", columnList = "noteType"),
})
@AuditTable
public class NotesMetadata extends TemporalCore<NotesMetadata, NotesMetadataAudit> {
    /**
     * \brief Unique identifier in EMAP for this NotesMetadata record.
     * <p>
     * This is the primary key for the NotesMetadata table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long notesMetadataId;

    /**
     * \brief Identifier for the HospitalVisit associated with this record.
     * <p>
     * This is a foreign key that joins the locationVisit table to the HospitalVisit table.
     */
    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    /**
     * \brief The type of note (ADMIT, TRANSFER, DISCHARGE). to be decided
     */
    private String noteType;

    /**
     * \brief The date and time that the note was created.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant startedDatetime;

    /**
     * \brief The date and time that the note was last edited.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant lastEditDatetime;

    /**
     * \brief The role of the person creating/editing the note.
     */
    private String editorRole;

    /**
     * Minimal constructor.
     * @param hospitalVisitId associated hospital visit
     * @param noteType       type of note
     * @param editorRole       type of note
     */
    public NotesMetadata(HospitalVisit hospitalVisitId, String noteType, String editorRole) {
        this.hospitalVisitId = hospitalVisitId;
        this.noteType = noteType;
        this.editorRole = editorRole;
    }

    /**
     * Copy constructor.
     * @param other entity to copy
     */
    private NotesMetadata(NotesMetadata other) {
        super(other);
        notesMetadataId = other.notesMetadataId;
        hospitalVisitId = other.hospitalVisitId;
        noteType = other.noteType;
        editorRole = other.editorRole;
        startedDatetime = other.startedDatetime;
        lastEditDatetime = other.lastEditDatetime;
    }

    @Override
    public NotesMetadata copy() {
        return new NotesMetadata(this);
    }

    @Override
    public NotesMetadataAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new NotesMetadataAudit(this, validUntil, storedUntil);
    }
}
