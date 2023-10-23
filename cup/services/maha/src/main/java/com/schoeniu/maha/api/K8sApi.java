package com.schoeniu.maha.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.util.PatchUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kubernetes API
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class K8sApi {

    private final static String DEPLOYMENT_SCALE_PATCH =
            "[{\"op\":\"replace\",\"path\":\"/spec/replicas\",\"value\":%d}]";

    private final AppsV1Api appsV1Api;

    @Value("${kubernetes.config.namespace}")
    private String namespace;

    /**
     * Scales given deployment to given number of replicas
     *
     * @param deployment deployment to scale
     * @param replicas   number of replicas to scale to
     */
    public void scaleDeployment(final String deployment, final int replicas) {
        try {
            PatchUtils.patch(V1Deployment.class,
                             () -> appsV1Api.patchNamespacedDeploymentCall(deployment,
                                                                           namespace,
                                                                           new V1Patch(String.format(
                                                                                   DEPLOYMENT_SCALE_PATCH,
                                                                                   replicas)),
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null),
                             V1Patch.PATCH_FORMAT_JSON_PATCH,
                             appsV1Api.getApiClient());
            log.info("Scaled {} to {} replicas.", deployment, replicas);
        } catch (ApiException e) {
            log.error(e.toString());
            log.error(e.getMessage());
            log.error(e.getResponseBody());
            e.printStackTrace();
        }
    }

    /**
     * Gets map of the number of pods every deployment in the cup namespace currently has.
     *
     * @return map with deployment name as key and number of pods as value.
     */
    public Map<String, Integer> getReplicasPerDeployment() {
        V1DeploymentList deploymentList = null;
        try {
            deploymentList = appsV1Api.listNamespacedDeployment("cup",
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                10,
                                                                false);
        } catch (ApiException e) {
            log.error(e.toString());
            log.error(e.getMessage());
            log.error(e.getResponseBody());
            e.printStackTrace();
        }
        Map<String, Integer> result = new HashMap<>();
        assert deploymentList != null;
        deploymentList.getItems()
                      .forEach(d -> {
                          String name = Objects.requireNonNull(d.getMetadata())
                                               .getName();
                          assert !result.containsKey(name);
                          result.put(name,
                                     Objects.requireNonNull(d.getSpec())
                                            .getReplicas());
                      });
        return result;
    }

}
