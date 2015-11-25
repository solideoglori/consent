package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ElectionMapper implements ResultSetMapper<Election> {

    public Election map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        UseRestriction useRestriction = null;

        if (r.getString("translatedUseRestriction") != null){
            try {
                useRestriction = UseRestriction.parse(r.getString("useRestriction"));
            } catch (IOException e) {
                throw new SQLException(e);
            }
        }

        return new Election(
                r.getInt("electionId"),
                r.getString("electionType"),
                (r.getString("finalVote") == null) ? null : r.getBoolean("finalVote"),
                r.getString("finalRationale"),
                r.getString("status"),
                r.getDate("createDate"),
                r.getDate("finalVoteDate"),
                r.getString("referenceId"),
                (r.getDate("lastUpdate") == null) ? null : r.getDate("lastUpdate"),
                (r.getString("finalAccessVote") == null) ? null : r.getBoolean("finalAccessVote"),
                useRestriction,
                (r.getString("translatedUseRestriction") == null) ? null : r.getString("translatedUseRestriction")
        );
    }
}