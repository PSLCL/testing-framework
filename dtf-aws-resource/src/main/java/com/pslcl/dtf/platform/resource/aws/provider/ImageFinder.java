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
package com.pslcl.dtf.platform.resource.aws.provider;


import java.util.ArrayList;
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
import com.pslcl.dtf.platform.core.runner.config.RunnerConfig;
import com.pslcl.dtf.platform.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.platform.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.platform.core.util.PropertiesFile;
import com.pslcl.dtf.platform.core.util.StrH;
import com.pslcl.dtf.platform.core.util.StrH.StringPair;
import com.pslcl.dtf.platform.resource.aws.attr.AwsNames;

public class ImageFinder
{
    private final Logger log;
    private final Properties defaultImageFilters;
    private final List<String> defaultLocationFilters;
    private volatile String defaultLocationYear;
    private volatile String defaultLocationMonth;
    private volatile String defaultLocationDot;
    private volatile RunnerConfig config;

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
        addImageFilter(AwsNames.ImageArchitectureKey, AwsNames.ImageArchitectureDefault);
        addImageFilter(AwsNames.ImageHypervisorKey, AwsNames.ImageHypervisorDefault);
        addImageFilter(AwsNames.ImageImageIdKey, AwsNames.ImageImageIdDefault);
        addImageFilter(AwsNames.ImageImageTypeKey, AwsNames.ImageImageTypeDefault);
        addImageFilter(AwsNames.ImageIsPublicKey, AwsNames.ImageIsPublicDefault);
        addImageFilter(AwsNames.ImageNameKey, AwsNames.ImageNameDefault);
        addImageFilter(AwsNames.ImageOwnerKey, AwsNames.ImageOwnerDefault);
        addImageFilter(AwsNames.ImagePlatformKey, AwsNames.ImagePlatformDefault);
        addImageFilter(AwsNames.ImageRootDevTypeKey, AwsNames.ImageRootDevTypeDefault);
        addImageFilter(AwsNames.ImageStateKey, AwsNames.ImageStateDefault);
        addImageFilter(AwsNames.BlockingDeviceDeleteOnTerminationKey, AwsNames.BlockingDeviceDeleteOnTerminationDefault);
        addImageFilter(AwsNames.BlockingDeviceVolumeTypeKey, AwsNames.BlockingDeviceVolumeTypeDefault);
        addImageFilter(AwsNames.BlockingDeviceVolumeSizeKey, AwsNames.BlockingDeviceVolumeSizeDefault);
        config.initsb.level.decrementAndGet();
        
        config.initsb.ttl("AWS Image Location Filters:");
        config.initsb.level.incrementAndGet();
        defaultLocationYear = config.properties.getProperty(AwsNames.LocationYearKey, AwsNames.LocationYearDefault);
        config.initsb.ttl(AwsNames.LocationYearKey, " = ", defaultLocationYear);
        defaultLocationMonth = config.properties.getProperty(AwsNames.LocationMonthKey, AwsNames.LocationMonthDefault);
        config.initsb.ttl(AwsNames.LocationMonthKey, " = ", defaultLocationMonth);
        defaultLocationDot = config.properties.getProperty(AwsNames.LocationDotKey, AwsNames.LocationDotDefault);
        config.initsb.ttl(AwsNames.LocationDotKey, " = ", defaultLocationDot);
        
        addLocationFilters();
        config.initsb.level.decrementAndGet();
    }        

    private void addImageFilter(String key, String defaultValue)
    {
        String value = config.properties.getProperty(key, defaultValue);
        config.initsb.ttl(key, " = ", value);
        if(value == null)
            return;
        defaultImageFilters.setProperty(key, value);
    }
    
    private void addLocationFilters()
    {
        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(AwsNames.LocationFeatureKey, config.properties);
        int size = list.size();
        if(size == 0)
        {
            // add the default
            StringPair pair = new StringPair(AwsNames.LocationFeatureKey+"0", AwsNames.LocationBase);
            list.add(pair);
            pair = new StringPair(AwsNames.LocationFeatureKey+"1", AwsNames.LocationHvm);
            list.add(pair);
            size += 2;
        }
        for(int i=0; i < size; i++)
        {
            Entry<String,String> entry = list.get(i);
            config.initsb.ttl(entry.getKey(), " = ", entry.getValue());
            defaultLocationFilters.add(entry.getValue());
        }
    }
    
    /**
     * Return an Image ID.
     * @param ec2Client the amazon client to use.
     * @param resource the resource being requested.
     * @return an image ID meeting all the filtering criteria. 
     * @throws ResourceNotFoundException if the criteria can not be meet.
     */
    @SuppressWarnings("null")
    public String findImage(AmazonEC2Client ec2Client, ResourceDescription resource) throws ResourceNotFoundException
    {
        try
        {
            DescribeImagesRequest request = new DescribeImagesRequest();
            ImageFilterData imageFilterData = getFilters(resource);
            request.setFilters(imageFilterData.imageFilters);
            DescribeImagesResult result = ec2Client.describeImages(request);
            java.util.List<Image> images = result.getImages();
            List<Image> locations = new ArrayList<Image>();
            long timestamp = 0;
            Image latestImage = null;
            for(Image image : images)
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
                if(frags.length > 3)
                {
                    int index = frags[0].lastIndexOf('-');
                    if(index != -1)
                    {
                        String year = frags[0].substring(index+1);
                        String month = frags[1];
                        String dot = frags[2];
                        if(imageFilterData.locationYear != null && !imageFilterData.locationYear.equals(year))
                            continue;
                        if(imageFilterData.locationMonth != null && !imageFilterData.locationMonth.equals(month))
                            continue;
                        if(imageFilterData.locationDot != null && !imageFilterData.locationDot.equals(dot))
                            continue;
                        String description = frags[3];
                        frags = description.split("-");
                        int size = imageFilterData.locationFilters.size();
                        boolean[] fragsFound = new boolean[size];
                        for(int i = 1; i < frags.length; i++)
                        {
                            if(frags[i].startsWith("20"))
                            {
                                long t0 = Long.parseLong(frags[i]);
                                if(t0 > tstamp)
                                {
                                    tstamp = t0;
                                    limage = image;
                                }
                                break;
                            }
                            for(int j=0; j < size; j++)
                            {
                                String filter = imageFilterData.locationFilters.get(j);
                                if(filter.equals(frags[i]))
                                {
                                    fragsFound[j] = true;
                                    break;
                                }
                            }
                        }
                        boolean found = true;
                        for(int i=0; i < size; i++)
                        {
                            if(!fragsFound[i])
                            {
                                found = false;
                                break;
                            }
                        }
                        if(found)
                        {
                            if(tstamp > timestamp)
                            {
                                timestamp = tstamp;
                                latestImage = limage;
                            }
                            locations.add(image);
                        }
                    }// if year delimiter found
                } // if 4 or more frags
            } // outer for loop
            if(locations.size() == 0)
                throw new ResourceNotFoundException();
            int size = locations.size();
            log.debug(getClass().getSimpleName() + ".findImage, " + size + " images found");
//            if(locations.size() == 1)
//                return locations.get(0).getImageId();
            log.debug(getClass().getSimpleName() + ".findImage: " + latestImage.toString());
            return latestImage.getImageId();
        }
        catch(ResourceNotFoundException rnfe)
        {
            log.warn(getClass().getSimpleName() + ".findImage: no images found for given filters", rnfe);
            throw rnfe;
        }
        catch(Exception e)
        {
            log.warn(getClass().getSimpleName() + ".findImage: ec2 client exception", e);
            throw new ResourceNotFoundException("ec2 client exception", e);
        }
    }
    
    private ImageFilterData getFilters(ResourceDescription resource)
    {
        Map<String, String> attrs = resource.getAttributes();
        List<Filter> imageFilters = new ArrayList<Filter>();
        List<String> locationFilters = new ArrayList<String>();
        String locationYear = null;
        String locationMonth = null;
        String locationDot = null;
        
        if(addImageFilter(AwsNames.ImageImageIdKey, AwsNames.ImageImageIdFilter, attrs, imageFilters))
            return new ImageFilterData(imageFilters, locationFilters, locationYear, locationMonth, locationDot);
        
        if(addImageFilter(AwsNames.ImageNameKey, AwsNames.ImageNameFilter, attrs, imageFilters))
            return new ImageFilterData(imageFilters, locationFilters, locationYear, locationMonth, locationDot);
        
        addImageFilter(AwsNames.ImageArchitectureKey, AwsNames.ImageArchitectureFilter, attrs, imageFilters);
        addImageFilter(AwsNames.ImageHypervisorKey, AwsNames.ImageHypervisorFilter, attrs, imageFilters);
        addImageFilter(AwsNames.ImageImageTypeKey, AwsNames.ImageImageTypeFilter, attrs, imageFilters);
        addImageFilter(AwsNames.ImageIsPublicKey, AwsNames.ImageIsPublicFilter, attrs, imageFilters);
//        addImageFilter(AwsNames.ImageNameKey, AwsNames.ImageNameFilter, attrs, imageFilters);
        addImageFilter(AwsNames.ImageOwnerKey, AwsNames.ImageOwnerFilter, attrs, imageFilters);
        addImageFilter(AwsNames.ImagePlatformKey, AwsNames.ImagePlatformFilter, attrs, imageFilters);
        addImageFilter(AwsNames.ImageRootDevTypeKey, AwsNames.ImageRootDevTypeFilter, attrs, imageFilters);
        addImageFilter(AwsNames.ImageStateKey, AwsNames.ImageStateFilter, attrs, imageFilters);
        addImageFilter(AwsNames.BlockingDeviceDeleteOnTerminationKey, AwsNames.BlockingDeviceDeleteOnTerminationFilter, attrs, imageFilters);
        addImageFilter(AwsNames.BlockingDeviceVolumeTypeKey, AwsNames.BlockingDeviceVolumeTypeFilter, attrs, imageFilters);
        addImageFilter(AwsNames.BlockingDeviceVolumeSizeKey, AwsNames.BlockingDeviceVolumeSizeFilter, attrs, imageFilters);
        
        locationYear = config.properties.getProperty(AwsNames.LocationYearKey, defaultLocationYear);
        locationMonth = config.properties.getProperty(AwsNames.LocationMonthKey, defaultLocationMonth);
        locationDot = config.properties.getProperty(AwsNames.LocationDotKey, defaultLocationDot);
        
        List<Entry<String, String>> flist = PropertiesFile.getPropertiesForBaseKey(AwsNames.LocationFeatureKey, attrs);
        for(Entry<String,String> entry : flist)
            locationFilters.add(entry.getValue());
        if(flist.size() == 0)
            locationFilters = defaultLocationFilters;
        return new ImageFilterData(imageFilters, locationFilters, locationYear, locationMonth, locationDot);
    }
    
    private boolean addImageFilter(String key, String filterName, Map<String, String> attrs, List<Filter> imageFilters)
    {
        // use the resource.attrs if given, otherwise inject defaults
        String value = attrs.get(key);
        if(value == null)
            value = defaultImageFilters.getProperty(key);
        if(value == null)
            return false;
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
    
}