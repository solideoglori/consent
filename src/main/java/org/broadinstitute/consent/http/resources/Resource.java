package org.broadinstitute.consent.http.resources;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.exceptions.UpdateConsentException;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by egolin on 9/17/14.
 * <p/>
 * Abstract superclass for all Resources.
 */
abstract public class Resource {

    // Resource based role names
    public final static String ADMIN = "Admin";
    public final static String ALUMNI = "Alumni";
    public final static String CHAIRPERSON = "Chairperson";
    public final static String DATAOWNER = "DataOwner";
    public final static String MEMBER = "Member";
    public final static String RESEARCHER = "Researcher";

    protected Logger logger() {
        return Logger.getLogger(this.getClass().getName());
    }

    protected Response createExceptionResponse(Exception e) {
        try {
            logger().warn("Returning error response to client: " + e.getMessage());
            ExceptionHandler handler = dispatch.get(e.getClass());
            if (handler != null) {
                return handler.handle(e);
            } else {
                return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
            }
        } catch (Throwable t) {
            logger().error(t.getMessage());
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    StreamingOutput createStreamingOutput(InputStream inputStream) {
        return output -> {
            try {
                output.write(IOUtils.toByteArray(inputStream));
            } catch (Exception e) {
                logger().error(e);
                throw e;
            }
        };
    }

    private interface ExceptionHandler {
        Response handle(Exception e);
    }

    private static final Map<Class, ExceptionHandler> dispatch = new HashMap<>();

    static {
        dispatch.put(UserRoleHandlerException.class, e ->
                Response.status(Response.Status.CONFLICT).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build());
        dispatch.put(UnsupportedOperationException.class, e ->
                Response.status(Response.Status.CONFLICT).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build());
        dispatch.put(IllegalArgumentException.class, e ->
                Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build());
        dispatch.put(IOException.class, e ->
                Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build());
        dispatch.put(BadRequestException.class, e ->
                Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build());
        dispatch.put(NotAuthorizedException.class, e ->
                Response.status(Response.Status.UNAUTHORIZED).entity(new Error(e.getMessage(), Response.Status.UNAUTHORIZED.getStatusCode())).build());
        dispatch.put(ForbiddenException.class, e ->
                Response.status(Response.Status.FORBIDDEN).entity(new Error(e.getMessage(), Response.Status.FORBIDDEN.getStatusCode())).build());
        dispatch.put(NotFoundException.class, e ->
                Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build());
        dispatch.put(UpdateConsentException.class, e ->
                Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build());
        dispatch.put(MySQLSyntaxErrorException.class, e ->
                Response.serverError().entity(new Error("Database Error", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build());
        dispatch.put(SQLSyntaxErrorException.class, e ->
                Response.serverError().entity(new Error("Database Error", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build());
        dispatch.put(SQLException.class, e ->
                Response.serverError().entity(new Error("Database Error", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build());
        dispatch.put(Exception.class, e ->
                Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build());

    }


}
