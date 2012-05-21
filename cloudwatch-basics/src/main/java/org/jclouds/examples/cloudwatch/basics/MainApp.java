/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.examples.cloudwatch.basics;

import com.google.common.collect.Iterators;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.cloudwatch.AWSCloudWatchProviderMetadata;
import org.jclouds.aws.ec2.AWSEC2ProviderMetadata;
import org.jclouds.cloudwatch.CloudWatch;
import org.jclouds.cloudwatch.CloudWatchAsyncClient;
import org.jclouds.cloudwatch.CloudWatchClient;
import org.jclouds.cloudwatch.domain.Datapoint;
import org.jclouds.cloudwatch.domain.Dimension;
import org.jclouds.cloudwatch.domain.EC2Constants;
import org.jclouds.cloudwatch.domain.GetMetricStatistics;
import org.jclouds.cloudwatch.domain.GetMetricStatisticsResponse;
import org.jclouds.cloudwatch.domain.Namespaces;
import org.jclouds.cloudwatch.domain.Statistics;
import org.jclouds.cloudwatch.domain.Unit;
import org.jclouds.cloudwatch.features.MetricClient;
import org.jclouds.cloudwatch.options.ListMetricsOptions;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationScope;
import org.jclouds.rest.RestContext;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * Demonstrates the use of {@link org.jclouds.cloudwatch.features.MetricClient}.
 *
 * @author Jeremy Whitlock
 */
public class MainApp {

   public static String INVALID_SYNTAX = "Invalid number of parameters. Syntax is: accesskeyid secretkey";
   public static int PARAMETERS = 2;

   public static void main(String[] args) {

      if (args.length < PARAMETERS) {
         throw new IllegalArgumentException(INVALID_SYNTAX);
      }

      // Arguments
      String accessKeyId = args[0];
      String secretKey = args[1];

      ComputeServiceContext awsEC2Context = null;
      RestContext<CloudWatchClient, CloudWatchAsyncClient> cloudWatchContext = null;

      try {
         cloudWatchContext = ContextBuilder.newBuilder(new AWSCloudWatchProviderMetadata())
                                           .credentials(accessKeyId, secretKey)
                                           .build();
         awsEC2Context = ContextBuilder.newBuilder(new AWSEC2ProviderMetadata())
                                       .credentials(accessKeyId, secretKey)
                                       .build(ComputeServiceContext.class);

         // Get all nodes
         Set<? extends ComputeMetadata> allNodes = awsEC2Context.getComputeService().listNodes();

         for (ComputeMetadata node : allNodes) {
            String nodeId = node.getProviderId();
            String region = getRegion(node.getLocation());
            MetricClient metricClient = cloudWatchContext.getApi().getMetricClientForRegion(region);
            int metricsCount = getMetricsCountForInstance(cloudWatchContext.getApi(), region, nodeId);
            double[] cpuUtilization = getCPUUtilizationStatsForInstanceOverTheLast24Hours(metricClient, nodeId);
            String cpuUtilizationHeader = "  CPU utilization statistics: ";
            DecimalFormat df = new DecimalFormat("#.##");

            System.out.println(nodeId + " CloudWatch Metrics (Past 24 hours)");
            System.out.println("  Total metrics stored: " + metricsCount);

            if (cpuUtilization == null) {
               System.out.println(cpuUtilizationHeader + "Unable to compute as there are no CPU utilization " +
                                        "metrics stored.");
            } else {
               System.out.println(cpuUtilizationHeader +
                                        df.format(cpuUtilization[0]) + "% (avg), " +
                                        df.format(cpuUtilization[1]) + "% (max), " +
                                        df.format(cpuUtilization[2]) + "% (min)");
            }
         }
      } finally {
         if (awsEC2Context != null) {
            awsEC2Context.close();
         }
         if (cloudWatchContext != null) {
            cloudWatchContext.close();
         }
      }

   }

   /**
    * Returns the count of metrics stored for the given nodeId and region.
    *
    * @param cloudWatchClient the cloud watch client (Will use MetricsClient when Issue 922 is fixed)
    * @param region the region the instance is in
    * @param nodeId the instance id
    *
    * @return the total count of metrics stored for the given instance id and region
    */
   private static int getMetricsCountForInstance(CloudWatchClient cloudWatchClient, String region, String nodeId) {
      // Uses CloudWatchClient+region instead of MetricsClient because the pagination helper only works with
      // CloudWatchClient: http://code.google.com/p/jclouds/issues/detail?id=922
      return Iterators.size(CloudWatch.listMetrics(cloudWatchClient,
                                                   region,
                                                   ListMetricsOptions.builder()
                                                                     // Only return metrics for the given instance
                                                                     .dimension(new Dimension(
                                                                           EC2Constants.Dimension.INSTANCE_ID,
                                                                           nodeId))
                                                                     .build()).iterator());
   }

   /**
    * Return an array of doubles with the CPUUtilization {@link EC2Constants.MetricName#CPU_UTILIZATION}
    * average, maximum and minimum values in respective order over the last 24 hours.
    *
    * @param metricClient the {@link MetricClient} to use
    * @param nodeId the instance id whose CPUUtilization statistics we're intersted in calculating
    *
    * @return the array of doubles describe above or null if there are no CPUUtilization metrics stored for the given
    * instance id over the past 24 hours
    */
   private static double[] getCPUUtilizationStatsForInstanceOverTheLast24Hours(MetricClient metricClient,
                                                                               String nodeId) {

      Dimension instanceIdDimension = new Dimension(EC2Constants.Dimension.INSTANCE_ID, nodeId);
      ListMetricsOptions lmOptions = ListMetricsOptions.builder()
                                                       // Only return metrics if they are CPUUtilization
                                                       .metricName(EC2Constants.MetricName.CPU_UTILIZATION)
                                                       // Only return metrics for the AWS/EC2 namespace
                                                       .namespace(Namespaces.EC2)
                                                       // Only return metrics for the given instance
                                                       .dimension(instanceIdDimension)
                                                       .build();

      // Return null to indicate there are no CPUUtilization metrics stored for the given node id
      if (Iterators.size(metricClient.listMetrics(lmOptions).iterator()) == 0) {
         return null;
      }

      Date endDate = new Date(); // Now
      Date startDate = new Date(endDate.getTime() - (1000 * 60 * 60 * 24)); // One day ago
      GetMetricStatistics statistics = GetMetricStatistics.builder()
                                                          // Specify the instance id you're interested in
                                                          .dimension(instanceIdDimension)
                                                          // Specify the metric name you're interested in
                                                          .metricName(EC2Constants.MetricName.CPU_UTILIZATION)
                                                          // Specify the namespace of the metric
                                                          .namespace(Namespaces.EC2)
                                                          // Populate the average statistic in the response
                                                          .statistic(Statistics.AVERAGE)
                                                          // Populate the maximum statistic in the response
                                                          .statistic(Statistics.MAXIMUM)
                                                          // Populate the minimum statistic in the response
                                                          .statistic(Statistics.MINIMUM)
                                                          // Specify the start time for the metric statistics you want
                                                          .startTime(startDate)
                                                          // Specify the end time for the metric statistics you want
                                                          .endTime(endDate)
                                                          // Specify the metric statistic granularity
                                                          .period(3600)
                                                          // Specify the unit the metric values should be in
                                                          .unit(Unit.PERCENT)
                                                          .build();
      GetMetricStatisticsResponse statisticsResponse = metricClient.getMetricStatistics(statistics);
      double avg = 0d;
      double max = 0d;
      double min = 0d;
      Iterator<Datapoint> datapointIterator = statisticsResponse.iterator();

      while(datapointIterator.hasNext()) {
         Datapoint datapoint = datapointIterator.next();
         Double dAvg = datapoint.getAverage();
         Double dMax = datapoint.getMaximum();
         Double dMin = datapoint.getMinimum();

         if (dAvg != null) {
            avg = ((avg + dAvg) / 2);
         }
         if (dMax != null) {
            if (dMax > max) {
               max = dMax;
            }
         }
         if (dMin != null) {
            if (dMin < min) {
               min = dMin;
            }
         }
      }

      return new double[]{avg, max, min};

   }

   /**
    * Returns the region as string for the given {@link Location}.
    *
    * @param location the location
    *
    * @return the region or null if the region cannot be found
    */
   private static String getRegion(Location location) {

      // Just to be safe
      if (location == null) {
         return null;
      }

      String region = null;
      while(region == null && location.getParent() != null) {
         if (location.getScope() == LocationScope.REGION) {
            region = location.getId();
         } else {
            location = location.getParent();
         }
      }
      return region;

   }

}
