package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.constants.KerberosConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.UserFederationProviderFactoryRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.services.NotFoundPlainTextException;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.timer.TimerProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedList;
import java.util.List;

/**
 * Base resource for managing users
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserFederationResource {
    protected static final Logger logger = Logger.getLogger(UserFederationResource.class);

    protected RealmModel realm;

    protected  RealmAuth auth;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    public UserFederationResource(RealmModel realm, RealmAuth auth) {
        this.auth = auth;
        this.realm = realm;

        auth.init(RealmAuth.Resource.USER);
    }

    /**
     * Get List of available provider factories
     *
     * @return
     */
    @GET
    @NoCache
    @Path("providers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserFederationProviderFactoryRepresentation> getProviders() {
        auth.requireView();
        List<UserFederationProviderFactoryRepresentation> providers = new LinkedList<UserFederationProviderFactoryRepresentation>();
        for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(UserFederationProvider.class)) {
            UserFederationProviderFactoryRepresentation rep = new UserFederationProviderFactoryRepresentation();
            rep.setId(factory.getId());
            rep.setOptions(((UserFederationProviderFactory)factory).getConfigurationOptions());
            providers.add(rep);
        }
        return providers;
    }

    /**
     * Get List of available provider factories
     *
     * @return
     */
    @GET
    @NoCache
    @Path("providers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserFederationProviderFactoryRepresentation getProvider(@PathParam("id") String id) {
        auth.requireView();
        for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(UserFederationProvider.class)) {
            if (!factory.getId().equals(id)) {
                continue;
            }
            UserFederationProviderFactoryRepresentation rep = new UserFederationProviderFactoryRepresentation();
            rep.setId(factory.getId());
            rep.setOptions(((UserFederationProviderFactory)factory).getConfigurationOptions());
            return rep;
        }
        throw new NotFoundPlainTextException("Could not find provider");
    }

    /**
     * Create a provider
     *
     * @param rep
     * @return
     */
    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createProviderInstance(UserFederationProviderRepresentation rep) {
        auth.requireManage();
        String displayName = rep.getDisplayName();
        if (displayName != null && displayName.trim().equals("")) {
            displayName = null;
        }
        UserFederationProviderModel model = realm.addUserFederationProvider(rep.getProviderName(), rep.getConfig(), rep.getPriority(), displayName,
                rep.getFullSyncPeriod(), rep.getChangedSyncPeriod(), rep.getLastSync());
        new UsersSyncManager().refreshPeriodicSyncForProvider(session.getKeycloakSessionFactory(), session.getProvider(TimerProvider.class), model, realm.getId());
        checkKerberosCredential(model);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    /**
     * Update a provider
     *
     * @param id
     * @param rep
     */
    @PUT
    @Path("instances/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateProviderInstance(@PathParam("id") String id, UserFederationProviderRepresentation rep) {
        auth.requireManage();
        String displayName = rep.getDisplayName();
        if (displayName != null && displayName.trim().equals("")) {
            displayName = null;
        }
        UserFederationProviderModel model = new UserFederationProviderModel(id, rep.getProviderName(), rep.getConfig(), rep.getPriority(), displayName,
                rep.getFullSyncPeriod(), rep.getChangedSyncPeriod(), rep.getLastSync());
        realm.updateUserFederationProvider(model);
        new UsersSyncManager().refreshPeriodicSyncForProvider(session.getKeycloakSessionFactory(), session.getProvider(TimerProvider.class), model, realm.getId());
        checkKerberosCredential(model);
    }

    /**
     * get a provider
     *
     * @param id
     */
    @GET
    @NoCache
    @Path("instances/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserFederationProviderRepresentation getProviderInstance(@PathParam("id") String id) {
        auth.requireView();
        for (UserFederationProviderModel model : realm.getUserFederationProviders()) {
            if (model.getId().equals(id)) {
                return ModelToRepresentation.toRepresentation(model);
            }
        }
        
        throw new NotFoundPlainTextException("could not find provider");
    }

    /**
     * Delete a provider
     *
     * @param id
     */
    @DELETE
    @Path("instances/{id}")
    public void deleteProviderInstance(@PathParam("id") String id) {
        auth.requireManage();

        UserFederationProviderRepresentation rep = getProviderInstance(id);
        UserFederationProviderModel model = new UserFederationProviderModel(id, null, null, -1, null, -1, -1, 0);
        realm.removeUserFederationProvider(model);
        new UsersSyncManager().removePeriodicSyncForProvider(session.getProvider(TimerProvider.class), model);
    }


    /**
     * list configured providers
     *
     * @return
     */
    @GET
    @Path("instances")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<UserFederationProviderRepresentation> getUserFederationInstances() {
        auth.requireManage();
        List<UserFederationProviderRepresentation> reps = new LinkedList<UserFederationProviderRepresentation>();
        for (UserFederationProviderModel model : realm.getUserFederationProviders()) {
            UserFederationProviderRepresentation rep = ModelToRepresentation.toRepresentation(model);
            reps.add(rep);
        }
        return reps;
    }

    /**
     * trigger sync of users
     *
     * @return
     */
    @GET
    @Path("sync/{id}")
    @NoCache
    public Response syncUsers(@PathParam("id") String providerId, @QueryParam("action") String action) {
        logger.debug("Syncing users");
        auth.requireManage();

        for (UserFederationProviderModel model : realm.getUserFederationProviders()) {
            if (model.getId().equals(providerId)) {
                UsersSyncManager syncManager = new UsersSyncManager();
                if ("triggerFullSync".equals(action)) {
                    syncManager.syncAllUsers(session.getKeycloakSessionFactory(), realm.getId(), model);
                } else if ("triggerChangedUsersSync".equals(action)) {
                    syncManager.syncChangedUsers(session.getKeycloakSessionFactory(), realm.getId(), model);
                }
                return Response.noContent().build();
            }
        }

        throw new NotFoundPlainTextException("could not find provider");
    }

    // Automatically add "kerberos" to required realm credentials if it's supported by saved provider
    private void checkKerberosCredential(UserFederationProviderModel model) {
        String allowKerberosCfg = model.getConfig().get(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION);
        if (Boolean.valueOf(allowKerberosCfg)) {
            boolean found = false;
            List<RequiredCredentialModel> currentCreds = realm.getRequiredCredentials();
            for (RequiredCredentialModel cred : currentCreds) {
                if (cred.getType().equals(UserCredentialModel.KERBEROS)) {
                    found = true;
                }
            }

            if (!found) {
                realm.addRequiredCredential(UserCredentialModel.KERBEROS);
                logger.info("Added 'kerberos' to required realm credentials");
            }
        }
    }

}
