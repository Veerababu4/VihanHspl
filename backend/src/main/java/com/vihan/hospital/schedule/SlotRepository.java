package com.vihan.hospital.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Slot changes made by the reconciler use conditional updates ("... and status = 'OPEN'"), so a
 * slot that was booked a moment ago by another request is never removed or blocked by mistake.
 */
public interface SlotRepository extends JpaRepository<Slot, Long> {

	List<Slot> findByDoctorIdAndSlotDateBetweenOrderByStartAt(Long doctorId, LocalDate from, LocalDate to);

	@Query("select max(s.slotDate) from Slot s where s.doctorId = :doctorId")
	Optional<LocalDate> findLastSlotDate(@Param("doctorId") Long doctorId);

	@Query("select max(s.tokenNo) from Slot s where s.doctorId = :doctorId and s.branchId = :branchId and s.slotDate = :date")
	Optional<Integer> findMaxToken(@Param("doctorId") Long doctorId, @Param("branchId") Long branchId,
			@Param("date") LocalDate date);

	/** Any slot of this doctor (any branch) overlapping [start, end): a doctor is in one place at a time. */
	@Query("select count(s) > 0 from Slot s where s.doctorId = :doctorId and s.startAt < :end and s.endAt > :start")
	boolean existsOverlapping(@Param("doctorId") Long doctorId, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			delete from slot s where s.slot_id = :id and s.status = 'OPEN'
			  and not exists (select 1 from appointment a where a.slot_id = s.slot_id)""", nativeQuery = true)
	int deleteIfOpenAndUnreferenced(@Param("id") Long id);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Slot s set s.status = com.vihan.hospital.schedule.Slot.Status.BLOCKED, s.blockReason = :reason,
			    s.version = s.version + 1
			where s.id = :id and s.status = com.vihan.hospital.schedule.Slot.Status.OPEN""")
	int blockIfOpen(@Param("id") Long id, @Param("reason") Slot.BlockReason reason);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Slot s set s.blockReason = :reason, s.version = s.version + 1
			where s.id = :id and s.status = com.vihan.hospital.schedule.Slot.Status.BLOCKED""")
	int changeBlockReason(@Param("id") Long id, @Param("reason") Slot.BlockReason reason);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Slot s set s.status = com.vihan.hospital.schedule.Slot.Status.OPEN, s.blockReason = null,
			    s.tokenNo = :token, s.version = s.version + 1
			where s.id = :id and s.status = com.vihan.hospital.schedule.Slot.Status.BLOCKED""")
	int reopenIfBlocked(@Param("id") Long id, @Param("token") int token);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Slot s set s.tokenNo = :token, s.version = s.version + 1
			where s.id = :id and s.status = com.vihan.hospital.schedule.Slot.Status.OPEN""")
	int retokenIfOpen(@Param("id") Long id, @Param("token") int token);

}
