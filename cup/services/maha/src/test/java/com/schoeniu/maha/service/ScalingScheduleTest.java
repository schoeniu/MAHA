package com.schoeniu.maha.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.schoeniu.maha.api.K8sApi;
import com.schoeniu.maha.api.SqsApi;
import com.schoeniu.maha.config.properties.ScalingConfigProperties;

import io.kubernetes.client.openapi.ApiClient;

@ActiveProfiles("test")
@SpringBootTest
class ScalingScheduleTest {

    @Autowired
    private ScalingConfigProperties scalingConfig;

    @MockBean
    private ApiClient apiClient;
    @MockBean
    private K8sApi k8SApi;
    @MockBean
    private SqsApi sqsApi;

    @Autowired
    private ScalingSchedule systemUnderTest;

    @Test
    void scheduleSingleOrigin() {
        //given
        scalingConfig.getStrategy()
                     .setFollowUpScalingEnabled(true);
        when(sqsApi.getNumberOfMessages(any())).thenReturn(0);
        when(sqsApi.getNumberOfMessages("EXT_REQUEST")).thenReturn(1000);

        Map<String, Integer> currentScale = new HashMap<>();
        scalingConfig.getQueuesConsumedFrom()
                     .forEach((queue, scalingConfig) -> currentScale.put(scalingConfig.getServiceName(), 1));
        when(k8SApi.getReplicasPerDeployment()).thenReturn(currentScale);

        //when
        systemUnderTest.schedule();

        //then

        verify(k8SApi).getReplicasPerDeployment();
        verify(k8SApi).scaleDeployment("cup-trigger", 5);
        verify(k8SApi).scaleDeployment("cup-process", 5);
        verify(k8SApi).scaleDeployment("cup-cache", 5);
        verify(k8SApi).scaleDeployment("cup-history", 17);
        verify(k8SApi).scaleDeployment("cup-vehicle-data", 4);
        verify(k8SApi).scaleDeployment("cup-rollout", 5);

    }

    @Test
    void scheduleMultipleOrigins() {
        //given
        scalingConfig.getStrategy()
                     .setFollowUpScalingEnabled(true);
        when(sqsApi.getNumberOfMessages(any())).thenReturn(0);
        when(sqsApi.getNumberOfMessages("EXT_REQUEST")).thenReturn(1000);
        when(sqsApi.getNumberOfMessages("CACHE_RESPONSE")).thenReturn(500);

        Map<String, Integer> currentScale = new HashMap<>();
        scalingConfig.getQueuesConsumedFrom()
                     .forEach((queue, scalingConfig) -> currentScale.put(scalingConfig.getServiceName(), 1));
        when(k8SApi.getReplicasPerDeployment()).thenReturn(currentScale);

        //when
        systemUnderTest.schedule();

        //then
        verify(k8SApi).getReplicasPerDeployment();
        verify(k8SApi).scaleDeployment("cup-trigger", 5);
        verify(k8SApi).scaleDeployment("cup-process", 7);
        verify(k8SApi).scaleDeployment("cup-cache", 5);
        verify(k8SApi).scaleDeployment("cup-history", 20);
        verify(k8SApi).scaleDeployment("cup-vehicle-data", 5);
        verify(k8SApi).scaleDeployment("cup-rollout", 7);

    }

    @Test
    void scheduleMultipleNoPredictiveScaling() {
        //given
        scalingConfig.getStrategy()
                     .setFollowUpScalingEnabled(false);
        when(sqsApi.getNumberOfMessages(any())).thenReturn(0);
        when(sqsApi.getNumberOfMessages("EXT_REQUEST")).thenReturn(1000);
        when(sqsApi.getNumberOfMessages("CACHE_RESPONSE")).thenReturn(500);
        Map<String, Integer> currentScale = new HashMap<>();
        scalingConfig.getQueuesConsumedFrom()
                     .forEach((queue, scalingConfig) -> currentScale.put(scalingConfig.getServiceName(), 1));
        when(k8SApi.getReplicasPerDeployment()).thenReturn(currentScale);

        //when
        systemUnderTest.schedule();

        //then
        verify(k8SApi).getReplicasPerDeployment();
        verify(k8SApi).scaleDeployment("cup-trigger", 5);
        verify(k8SApi).scaleDeployment("cup-process", 3);
        verify(k8SApi, never()).scaleDeployment(eq("cup-cache"), anyInt());
        verify(k8SApi, never()).scaleDeployment(eq("cup-vehicle-data"), anyInt());
        verify(k8SApi, never()).scaleDeployment(eq("cup-rollout"), anyInt());
        verify(k8SApi, never()).scaleDeployment(eq("cup-history"), anyInt());

    }

    @Test
    void scheduleSelectHighestScaling() {
        //given
        scalingConfig.getStrategy()
                     .setFollowUpScalingEnabled(false);
        when(sqsApi.getNumberOfMessages(any())).thenReturn(0);
        when(sqsApi.getNumberOfMessages("CACHE_RESPONSE")).thenReturn(0);
        when(sqsApi.getNumberOfMessages("VEHICLE_DATA_RESPONSE")).thenReturn(0);
        when(sqsApi.getNumberOfMessages("TRIGGER")).thenReturn(800);
        Map<String, Integer> currentScale = new HashMap<>();
        scalingConfig.getQueuesConsumedFrom()
                     .forEach((queue, scalingConfig) -> currentScale.put(scalingConfig.getServiceName(), 1));
        currentScale.put("cup-process", 4);
        when(k8SApi.getReplicasPerDeployment()).thenReturn(currentScale);

        //when
        systemUnderTest.schedule();

        //then
        verify(k8SApi, never()).scaleDeployment(eq("cup-process"), anyInt());

    }

    @Test
    void scheduleSelectHighestScaling2() {
        //given
        scalingConfig.getStrategy()
                     .setFollowUpScalingEnabled(false);
        when(sqsApi.getNumberOfMessages(any())).thenReturn(0);
        when(sqsApi.getNumberOfMessages("CACHE_RESPONSE")).thenReturn(0);
        when(sqsApi.getNumberOfMessages("VEHICLE_DATA_RESPONSE")).thenReturn(0);
        when(sqsApi.getNumberOfMessages("TRIGGER")).thenReturn(800);
        Map<String, Integer> currentScale = new HashMap<>();
        scalingConfig.getQueuesConsumedFrom()
                     .forEach((queue, scalingConfig) -> currentScale.put(scalingConfig.getServiceName(), 1));
        currentScale.put("cup-process", 3);
        when(k8SApi.getReplicasPerDeployment()).thenReturn(currentScale);

        //when
        systemUnderTest.schedule();

        //then
        verify(k8SApi).scaleDeployment("cup-process", 4);
        verify(k8SApi, never()).scaleDeployment("cup-process", 3);
        verify(k8SApi, never()).scaleDeployment("cup-process", 1);

    }

    @Test
    void scheduleDownscale2to1() {
        //given
        scalingConfig.getStrategy()
                     .setFollowUpScalingEnabled(false);
        when(sqsApi.getNumberOfMessages(any())).thenReturn(0);
        when(sqsApi.getNumberOfMessages("TRIGGER")).thenReturn(50);
        Map<String, Integer> currentScale = new HashMap<>();
        scalingConfig.getQueuesConsumedFrom()
                     .forEach((queue, scalingConfig) -> currentScale.put(scalingConfig.getServiceName(), 1));
        currentScale.put("cup-process", 2);
        when(k8SApi.getReplicasPerDeployment()).thenReturn(currentScale);

        //when
        systemUnderTest.schedule();

        //then
        verify(k8SApi).scaleDeployment("cup-process", 1);

    }

    @Test
    void scheduleDownscaleStaysAtTwoWhenNotBelowHalfRateOfOne() {
        //given
        scalingConfig.getStrategy()
                     .setFollowUpScalingEnabled(false);
        when(sqsApi.getNumberOfMessages(any())).thenReturn(0);
        when(sqsApi.getNumberOfMessages("TRIGGER")).thenReturn(60);
        Map<String, Integer> currentScale = new HashMap<>();
        scalingConfig.getQueuesConsumedFrom()
                     .forEach((queue, scalingConfig) -> currentScale.put(scalingConfig.getServiceName(), 1));
        currentScale.put("cup-process", 2);
        when(k8SApi.getReplicasPerDeployment()).thenReturn(currentScale);

        //when
        systemUnderTest.schedule();

        //then
        verify(k8SApi, never()).scaleDeployment(eq("cup-process"), anyInt());

    }
}