/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.resource.aws.provider.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.core.util.PropertiesFile;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.StrH.StringPair;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.attr.ClientNames;
import com.pslcl.dtf.resource.aws.attr.InstanceNames;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;

@SuppressWarnings("javadoc")
public class ImageFinder
{
    private final Logger log;
    private final Properties defaultImageFilters;
    private final List<String> defaultLocationFilters;
    private volatile String defaultLocationYear;
    private volatile String defaultLocationMonth;
    private volatile String defaultLocationDot;
    private volatile String defaultImageId;
    private volatile String defaultPlatform;
    private volatile RunnerConfig config;
    private volatile List<GlobalImageAttrMapData> globalImageAttrMapData;

    public ImageFinder()
    {
        log = LoggerFactory.getLogger(getClass());
        defaultImageFilters = new Properties();
        defaultLocationFilters = new ArrayList<String>();
    }

    public void init(RunnerConfig config) throws Exception
    {
        this.config = config;
        config.initsb.ttl("AWS Image Filters:");
        config.initsb.level.incrementAndGet();
        addImageFilter(ProviderNames.ImageArchitectureKey, ProviderNames.ImageArchitectureDefault);
        addImageFilter(ProviderNames.ImageHypervisorKey, ProviderNames.ImageHypervisorDefault);
        addImageFilter(ResourceNames.ImageImageIdKey, ProviderNames.ImageImageIdDefault);
        addImageFilter(ProviderNames.ImageImageTypeKey, ProviderNames.ImageImageTypeDefault);
        addImageFilter(ProviderNames.ImageIsPublicKey, ProviderNames.ImageIsPublicDefault);
        addImageFilter(ProviderNames.ImageNameKey, ProviderNames.ImageNameDefault);
        addImageFilter(ProviderNames.ImageOwnerKey, ProviderNames.ImageOwnerDefault);
        addImageFilter(ProviderNames.ImagePlatformKey, ProviderNames.ImagePlatformDefault);
        addImageFilter(ProviderNames.ImageRootDevTypeKey, ProviderNames.ImageRootDevTypeDefault);
        addImageFilter(ProviderNames.ImageStateKey, ProviderNames.ImageStateDefault);
        addImageFilter(ProviderNames.BlockingDeviceDeleteOnTerminationKey, ProviderNames.BlockingDeviceDeleteOnTerminationDefault);
        addImageFilter(ProviderNames.BlockingDeviceVolumeTypeKey, ProviderNames.BlockingDeviceVolumeTypeDefault);
        addImageFilter(ProviderNames.BlockingDeviceVolumeSizeKey, ProviderNames.BlockingDeviceVolumeSizeDefault);
        config.initsb.level.decrementAndGet();

        config.initsb.ttl("AWS Image Location Filters:");
        config.initsb.level.incrementAndGet();
        defaultLocationYear = config.properties.getProperty(ProviderNames.LocationYearKey, ProviderNames.LocationYearDefault);
        defaultLocationYear = StrH.trim(defaultLocationYear);
        config.initsb.ttl(ProviderNames.LocationYearKey, " = ", defaultLocationYear);
        defaultLocationMonth = config.properties.getProperty(ProviderNames.LocationMonthKey, ProviderNames.LocationMonthDefault);
        defaultLocationMonth = StrH.trim(defaultLocationMonth);
        config.initsb.ttl(ProviderNames.LocationMonthKey, " = ", defaultLocationMonth);
        defaultLocationDot = config.properties.getProperty(ProviderNames.LocationDotKey, ProviderNames.LocationDotDefault);
        defaultLocationDot = StrH.trim(defaultLocationDot);
        config.initsb.ttl(ProviderNames.LocationDotKey, " = ", defaultLocationDot);

        defaultImageId = config.properties.getProperty(ResourceNames.ImageImageIdKey, ResourceNames.ImageImageIdDefault);
        defaultImageId = StrH.trim(defaultImageId);
        defaultPlatform = config.properties.getProperty(ResourceNames.ImageOsKey, ResourceNames.ImagePlatformDefault);
        defaultPlatform = StrH.trim(defaultPlatform);
        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(ResourceNames.ImageConfigKey, config.properties);
        if (list.size() > 0)
            globalImageAttrMapData = new ArrayList<GlobalImageAttrMapData>();
        for (Entry<String, String> entry : list)
        {
            /*
                image-id custom1=value1 custom2=value2 custom3=value3 pslcl.dtf.resource.machine.platform=windows 
                note aws requires the well known key pslcl.dtf.resource.machine.platform if it's default value is not desired
                ami-812406eb architecture=x86_64  
                ami-812406eb architecture=x86_64 pslcl.dtf.resource.machine.platform=windows  
                ami-315b7e5b architecture=x86_64 pslcl.dtf.resource.machine.platform=linux
            */
            String value = entry.getValue();
            value = StrH.trim(value);
            String[] attrs = value.split(" ");

            try
            {
                String imageId = attrs[0];
                Map<String, String> attrMap = new HashMap<String, String>();
                for (int i = 1; i < attrs.length; i++)
                {
                    String pair = attrs[i];
                    int idx = pair.indexOf('=');
                    String key = pair.substring(0, idx);
                    String avalue = pair.substring(++idx);
                    attrMap.put(key, avalue);
                }
                globalImageAttrMapData.add(new GlobalImageAttrMapData(imageId, attrMap));
            } catch (Exception e)
            {
                throw new Exception("invalid " + ResourceNames.ImageConfigKey + " format: " + value + " : " + e.getMessage());
            }
        }

        addLocationFilters();
        config.initsb.level.decrementAndGet();
    }

    private void addImageFilter(String key, String defaultValue)
    {
        String value = config.properties.getProperty(key, defaultValue);
        value = StrH.trim(value);
        config.initsb.ttl(key, " = ", value);
        if (value == null || value.length() == 0)
            return;
        defaultImageFilters.setProperty(key, value);
    }

    private void addLocationFilters()
    {
        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(ProviderNames.LocationFeatureKey, config.properties);
        int size = list.size();
        if (size == 0)
        {
            // add the default
            StringPair pair = new StringPair(ProviderNames.LocationFeatureKey + "0", ProviderNames.LocationBase);
            list.add(pair);
            pair = new StringPair(ProviderNames.LocationFeatureKey + "1", ProviderNames.LocationHvm);
            list.add(pair);
            size += 2;
        }
        for (int i = 0; i < size; i++)
        {
            Entry<String, String> entry = list.get(i);
            String value = entry.getValue();
            if (value == null || value.length() == 0)
                continue;
            config.initsb.ttl(entry.getKey(), " = ", value);
            defaultLocationFilters.add(value);
        }
    }

    private String checkTestscriptDeclared(ResourceDescription resource, TabToLevel format) throws ResourceNotFoundException
    {
        Map<String, String> customAttrs = new HashMap<String, String>();
        List<String> allKnownKeys = ResourceNames.getProviderKeys();
        allKnownKeys.addAll(ClientNames.getClientKeys());
        allKnownKeys.addAll(InstanceNames.getInstanceKeys());
        allKnownKeys.addAll(ProviderNames.getMachineKeys());
        allKnownKeys.addAll(ProviderNames.getNetworkKeys());
        format.ttl("custom bind attributes:");
        format.level.incrementAndGet();
        String msg = "Test script specified custom image attributes but no global mappings been configured for them";
        for (Entry<String, String> entry : resource.getAttributes().entrySet())
        {
            String key = entry.getKey();
            if (!allKnownKeys.contains(key) || key.equals(ResourceNames.ImageOsKey))
            {
                String value = entry.getValue();
                customAttrs.put(key, value);
                format.ttl(key, " = ", value);
            }
        }
        if (customAttrs.size() == 0)
        {
            format.ttl("none");
            return null;
        }

        if (globalImageAttrMapData == null)
        {
            format.ttl(msg);
            LoggerFactory.getLogger(getClass()).warn(format.toString());
            throw new ResourceNotFoundException(msg);
        }
        for (int i = 0; i < globalImageAttrMapData.size(); i++)
        {
            GlobalImageAttrMapData data = globalImageAttrMapData.get(i);
            if (data.isHit(customAttrs))
                return data.imageId;
        }
        throw new ResourceNotFoundException(msg);
    }

    /**
     * Return an Image ID.
     * @param ec2Client the amazon client to use.
     * @param resource the resource being requested.
     * @return an image ID meeting all the filtering criteria. 
     * @throws ResourceNotFoundException if the criteria can not be meet.
     */
    public String findImage(AmazonEC2Client ec2Client, ResourceDescription resource) throws ResourceNotFoundException
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\n", getClass().getSimpleName(), "findImage:");
        String imageId = checkTestscriptDeclared(resource, format);
        try
        {
            // imageId from checkTestscriptDeclared takes priority over aws specific filters
            ImageFilterData imageFilterData = getFilters(resource, imageId);
            DescribeImagesRequest request = new DescribeImagesRequest();
            request.setFilters(imageFilterData.imageFilters);
            DescribeImagesResult result = ec2Client.describeImages(request);
            java.util.List<Image> images = result.getImages();
            List<Image> locations = new ArrayList<Image>();
            
            long timestamp = 0;
            Image latestImage = null;
            for (Image image : images)
            {
                // see MAx@AWS's post at 
                // https://forums.aws.amazon.com/thread.jspa?messageID=376434
                // towards the bottom: 
                // "The Amazon Linux AMI strives for a major release each March and September, 
                // which is why you'll see our AMIs adopting a 2013.03, 2013.09, etc. naming system.
                // dot releases off those 6 month releases look like this: 2013.09.1 and 2013.09.2."
                // example: 
                // amazon/aws-elasticbeanstalk-amzn-2014.09.1.x86_64-python26-pv-201410222339

                long tstamp = 0;
                Image limage = null;
                String loc = image.getImageLocation();
                loc = StrH.getAtomicName(loc, '/');
                String[] frags = loc.split("\\.");
                if (images.size() == 1)
                    latestImage = image;
                if (frags.length > 3)
                {
                    int index = frags[0].lastIndexOf('-');
                    if (index != -1)
                    {
                        String year = frags[0].substring(index + 1);
                        String month = frags[1];
                        String dot = frags[2];
                        if (imageFilterData.locationYear != null && !imageFilterData.locationYear.equals(year))
                            continue;
                        if (imageFilterData.locationMonth != null && !imageFilterData.locationMonth.equals(month))
                            continue;
                        if (imageFilterData.locationDot != null && !imageFilterData.locationDot.equals(dot))
                            continue;
                        String description = frags[3];
                        frags = description.split("-");
                        int size = imageFilterData.locationFilters.size();
                        boolean[] fragsFound = new boolean[size];
                        for (int i = 1; i < frags.length; i++)
                        {
                            if (frags[i].startsWith("20"))
                            {
                                long t0 = Long.parseLong(frags[i]);
                                if (t0 > tstamp)
                                {
                                    tstamp = t0;
                                    limage = image;
                                }
                                break;
                            }
                            for (int j = 0; j < size; j++)
                            {
                                String filter = imageFilterData.locationFilters.get(j);
                                if (filter.equals(frags[i]))
                                {
                                    fragsFound[j] = true;
                                    break;
                                }
                            }
                        }
                        boolean found = true;
                        for (int i = 0; i < size; i++)
                        {
                            if (!fragsFound[i])
                            {
                                found = false;
                                break;
                            }
                        }
                        if (found)
                        {
                            if (tstamp > timestamp)
                            {
                                timestamp = tstamp;
                                latestImage = limage;
                            }
                            locations.add(image);
                        }
                    } // if year delimiter found
                } // if 4 or more frags
            } // outer for loop
              // images.size() == 1 on given image-id
            if (images.size() != 1 && locations.size() == 0)
                throw new ResourceNotFoundException();
            int size = locations.size();
            log.debug(getClass().getSimpleName() + ".findImage, " + size + " images found by location");
            //            if(locations.size() == 1)
            //                latestImage = image;
            //                return locations.get(0).getImageId();
            log.debug(getClass().getSimpleName() + ".findImage: " + latestImage.toString());
            return latestImage.getImageId();
        } catch (ResourceNotFoundException rnfe)
        {
            log.warn(getClass().getSimpleName() + ".findImage: no images found for given filters", rnfe);
            throw rnfe;
        } catch (Exception e)
        {
            log.warn(getClass().getSimpleName() + ".findImage: ec2 client exception", e);
            throw new ResourceNotFoundException("ec2 client exception", e);
        }
    }

    private ImageFilterData getFilters(ResourceDescription resource, String forceImageId)
    {
        List<Filter> imageFilters = new ArrayList<Filter>();
        List<String> locationFilters = new ArrayList<String>();
        String locationYear = null;
        String locationMonth = null;
        String locationDot = null;

        if(forceImageId != null)
        {
            List<String> args = new ArrayList<String>();
            args.add(forceImageId);
            Filter filter = new Filter(ProviderNames.ImageImageIdFilter, args);
            imageFilters.add(filter);
            return new ImageFilterData(imageFilters, locationFilters, locationYear, locationMonth, locationDot);
            
        }
        if (addImageFilter(ResourceNames.ImageImageIdKey, ProviderNames.ImageImageIdFilter, resource, imageFilters))
            return new ImageFilterData(imageFilters, locationFilters, locationYear, locationMonth, locationDot);

        if (addImageFilter(ProviderNames.ImageNameKey, ProviderNames.ImageNameFilter, resource, imageFilters))
            return new ImageFilterData(imageFilters, locationFilters, locationYear, locationMonth, locationDot);

        addImageFilter(ProviderNames.ImageArchitectureKey, ProviderNames.ImageArchitectureFilter, resource, imageFilters);
        addImageFilter(ProviderNames.ImageHypervisorKey, ProviderNames.ImageHypervisorFilter, resource, imageFilters);
        addImageFilter(ProviderNames.ImageImageTypeKey, ProviderNames.ImageImageTypeFilter, resource, imageFilters);
        addImageFilter(ProviderNames.ImageIsPublicKey, ProviderNames.ImageIsPublicFilter, resource, imageFilters);
        //        addImageFilter(AwsNames.ImageNameKey, AwsNames.ImageNameFilter, resource, imageFilters);
        addImageFilter(ProviderNames.ImageOwnerKey, ProviderNames.ImageOwnerFilter, resource, imageFilters);
        addImageFilter(ProviderNames.ImagePlatformKey, ProviderNames.ImagePlatformFilter, resource, imageFilters);
        addImageFilter(ProviderNames.ImageRootDevTypeKey, ProviderNames.ImageRootDevTypeFilter, resource, imageFilters);
        addImageFilter(ProviderNames.ImageStateKey, ProviderNames.ImageStateFilter, resource, imageFilters);
        addImageFilter(ProviderNames.BlockingDeviceDeleteOnTerminationKey, ProviderNames.BlockingDeviceDeleteOnTerminationFilter, resource, imageFilters);
        addImageFilter(ProviderNames.BlockingDeviceVolumeTypeKey, ProviderNames.BlockingDeviceVolumeTypeFilter, resource, imageFilters);
        addImageFilter(ProviderNames.BlockingDeviceVolumeSizeKey, ProviderNames.BlockingDeviceVolumeSizeFilter, resource, imageFilters);

        locationYear = config.properties.getProperty(ProviderNames.LocationYearKey, defaultLocationYear);
        locationMonth = config.properties.getProperty(ProviderNames.LocationMonthKey, defaultLocationMonth);
        locationDot = config.properties.getProperty(ProviderNames.LocationDotKey, defaultLocationDot);

        List<Entry<String, String>> flist = PropertiesFile.getPropertiesForBaseKey(ProviderNames.LocationFeatureKey, resource.getAttributes());
        for (Entry<String, String> entry : flist)
            locationFilters.add(entry.getValue());
        if (flist.size() == 0)
            locationFilters = defaultLocationFilters;
        return new ImageFilterData(imageFilters, locationFilters, locationYear, locationMonth, locationDot);
    }

    private boolean addImageFilter(String key, String filterName, ResourceDescription resource, List<Filter> imageFilters)
    {
        // use the resource.attrs if given, otherwise inject defaults
        String value = resource.getAttributes().get(key);
        if (value == null)
        {
            value = defaultImageFilters.getProperty(key);
            if (value != null)
                resource.addAttribute(key, value);
            else
                return false;
        }
        List<String> args = new ArrayList<String>();
        args.add(value);
        Filter filter = new Filter(filterName, args);
        imageFilters.add(filter);
        return true;
    }

    class ImageFilterData
    {
        public final List<Filter> imageFilters;
        public final List<String> locationFilters;
        public final String locationYear;
        public final String locationMonth;
        public final String locationDot;

        private ImageFilterData(List<Filter> imageFilters, List<String> locationFilters, String locationYear, String locationMonth, String locationDot)
        {
            this.imageFilters = imageFilters;
            this.locationFilters = locationFilters;
            this.locationYear = locationYear;
            this.locationMonth = locationMonth;
            this.locationDot = locationDot;
        }
    }

    class GlobalImageAttrMapData
    {
        private final String imageId;
        private final Map<String, String> attrMap;

        private GlobalImageAttrMapData(String imageId, Map<String, String> attrMap) throws Exception
        {
            this.imageId = imageId;
            this.attrMap = attrMap;
        }

        private boolean isHit(Map<String, String> attrs)
        {
            for (Entry<String, String> entry : attrs.entrySet())
            {
                String key = entry.getKey();
                if(!attrMap.containsKey(key))
                    return false;
                String givenValue = attrs.get(key);
                if (givenValue == null)
                    return false;
                if (!attrMap.get(key).equals(givenValue))
                    return false;
            }
            return true;
        }
    }
}