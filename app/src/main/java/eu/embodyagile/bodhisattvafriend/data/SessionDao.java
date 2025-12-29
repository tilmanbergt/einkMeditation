package eu.embodyagile.bodhisattvafriend.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SessionDao {

    @Insert
    long insert(SessionEntity session);

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    List<SessionEntity> getAllDesc();

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC LIMIT :limit")
    List<SessionEntity> getLatest(int limit);

    @Query("SELECT * FROM sessions WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    List<SessionEntity> getBetween(long from, long to);

    @Query("DELETE FROM sessions")
    void deleteAll();


    @Query("SELECT * FROM sessions ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    List<SessionEntity> getPageDesc(int limit, int offset);

    @Query("SELECT COUNT(*) FROM sessions")
    int countSessions();

    @Query("DELETE FROM sessions WHERE id = :id")
    int deleteById(long id);


    @Insert
    List<Long> insertAll(List<SessionEntity> entities);

}
