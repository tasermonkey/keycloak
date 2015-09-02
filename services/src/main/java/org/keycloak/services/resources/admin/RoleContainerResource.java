package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.NotFoundPlainTextException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleContainerResource extends RoleResource {
    private final RealmModel realm;
    private final RealmAuth auth;
    protected RoleContainerModel roleContainer;

    public RoleContainerResource(RealmModel realm, RealmAuth auth, RoleContainerModel roleContainer) {
        super(realm);
        this.realm = realm;
        this.auth = auth;
        this.roleContainer = roleContainer;
    }

    /**
     * List all roles for this realm or client
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<RoleRepresentation> getRoles() {
        auth.requireAny();

        Set<RoleModel> roleModels = roleContainer.getRoles();
        List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roleModels) {
            roles.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return roles;
    }

    /**
     * Create a new role for this realm or client
     *
     * @param uriInfo
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRole(final @Context UriInfo uriInfo, final RoleRepresentation rep) {
        auth.requireManage();

        try {
            RoleModel role = roleContainer.addRole(rep.getName());
            role.setDescription(rep.getDescription());
            return Response.created(uriInfo.getAbsolutePathBuilder().path(role.getName()).build()).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Role with name " + rep.getName() + " already exists");
        }
    }

    /**
     * Get a role by name
     *
     * @param roleName role's name (not id!)
     * @return
     */
    @Path("{role-name}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public RoleRepresentation getRole(final @PathParam("role-name") String roleName) {
        auth.requireView();

        RoleModel roleModel = roleContainer.getRole(roleName);
        if (roleModel == null) {
            throw new NotFoundPlainTextException("Could not find role: " + roleName);
        }

        return getRole(roleModel);
    }

    /**
     * Delete a role by name
     *
     * @param roleName role's name (not id!)
     */
    @Path("{role-name}")
    @DELETE
    @NoCache
    public void deleteRole(final @PathParam("role-name") String roleName) {
        auth.requireManage();

        RoleRepresentation rep = getRole(roleName);
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundPlainTextException("Could not find role: " + roleName);
        }
        deleteRole(role);
    }

    /**
     * Update a role by name
     *
     * @param roleName role's name (not id!)
     * @param rep
     * @return
     */
    @Path("{role-name}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRole(final @PathParam("role-name") String roleName, final RoleRepresentation rep) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundPlainTextException("Could not find role: " + roleName);
        }
        try {
            updateRole(rep, role);
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Role with name " + rep.getName() + " already exists");
        }
    }

    /**
     * Add a composite to this role
     *
     * @param roleName role's name (not id!)
     * @param roles
     */
    @Path("{role-name}/composites")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addComposites(final @PathParam("role-name") String roleName, List<RoleRepresentation> roles) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundPlainTextException("Could not find role: " + roleName);
        }
        addComposites(roles, role);
    }

    /**
     * List composites of this role
     *
     * @param roleName role's name (not id!)
     * @return
     */
    @Path("{role-name}/composites")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getRoleComposites(final @PathParam("role-name") String roleName) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundPlainTextException("Could not find role: " + roleName);
        }
        return getRoleComposites(role);
    }

    /**
     * Get realm-level roles of this role's composite
     *
     * @param roleName role's name (not id!)
     * @return
     */
    @Path("{role-name}/composites/realm")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getRealmRoleComposites(final @PathParam("role-name") String roleName) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundPlainTextException("Could not find role: " + roleName);
        }
        return getRealmRoleComposites(role);
    }

    /**
     * An client-level roles for a specific client for this role's composite
     *
     * @param roleName role's name (not id!)
     * @param clientId
     * @return
     */
    @Path("{role-name}/composites/client/{clientId}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getClientRoleComposites(final @PathParam("role-name") String roleName,
                                                           final @PathParam("clientId") String clientId) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundPlainTextException("Could not find role: " + roleName);
        }
        ClientModel app = realm.getClientByClientId(clientId);
        if (app == null) {
            throw new NotFoundPlainTextException("Could not find client: " + clientId);

        }
        return getClientRoleComposites(app, role);
    }


    /**
     * An app-level roles for a specific app for this role's composite
     *
     * @param roleName role's name (not id!)
     * @param id
     * @return
     */
    @Path("{role-name}/composites/client-by-id/{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getClientByIdRoleComposites(final @PathParam("role-name") String roleName,
                                                                final @PathParam("id") String id) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundPlainTextException("Could not find role: " + roleName);
        }
        ClientModel client = realm.getClientById(id);
        if (client == null) {
            throw new NotFoundPlainTextException("Could not find client: " + id);

        }
        return getClientRoleComposites(client, role);
    }


    /**
     * Remove roles from this role's composite
     *
     * @param roleName role's name (not id!)
     * @param roles roles to remove
     */
    @Path("{role-name}/composites")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteComposites(final @PathParam("role-name") String roleName, List<RoleRepresentation> roles) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundPlainTextException("Could not find role: " + roleName);
        }
        deleteComposites(roles, role);
    }


}
