/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.content.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.ContentAccessor;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.extensions.surf.util.ParameterCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides basic services for {@link org.alfresco.repo.content.transform.ContentTransformer}
 * implementations.
 * <p>
 * This class maintains the performance measures for the transformers as well, making sure that
 * there is an extra penalty for transformers that fail regularly.
 * 
 * @deprecated 
 * Deprecated since 3.0.  The abstract base class org.alfresco.repo.content.transform.AbstractContentTransformer2 should now be used instead.
 * 
 * @author Derek Hulley
 */
@Deprecated
public abstract class AbstractContentTransformer implements ContentTransformer, BeanNameAware
{
    private static final Log logger = LogFactory.getLog(AbstractContentTransformer.class);
    
    private MimetypeService mimetypeService;
    private ContentTransformerRegistry registry;
    @SuppressWarnings("deprecation")
    private List<ContentTransformerRegistry.TransformationKey> explicitTransformations;
    private double averageTime = 0.0;
    private long count = 0L;
    
    /** The bean name. */
    private String beanName;

    /**
     * All transformers start with an average transformation time of 0.0ms.
     */
    @SuppressWarnings("deprecation")
    protected AbstractContentTransformer()
    {
        averageTime = 0.0;
        explicitTransformations = new ArrayList<ContentTransformerRegistry.TransformationKey>(0);
    }

    /**
     * The registry to auto-register with
     * 
     * @param registry the transformer registry
     */
    public void setRegistry(ContentTransformerRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * Helper setter of the mimetype service.  This is not always required.
     * 
     * @param mimetypeService MimetypeService
     */
    public void setMimetypeService(MimetypeService mimetypeService)
    {
        this.mimetypeService = mimetypeService;
    }

    /**
     * @return Returns the mimetype helper
     */
    protected MimetypeService getMimetypeService()
    {
        return mimetypeService;
    }

    /**
     * @return Returns the explicit transformations that were enabled for this transformer
     */
    @SuppressWarnings("deprecation")
    protected List<ContentTransformerRegistry.TransformationKey> getExplicitTransformations()
    {
        return explicitTransformations;
    }

    /**
     * Set the transformations that this transformer can do regardless of what it returns
     * via the reliability check.
     * 
     * @param explicitTransformations explicit key mappings
     */
    @SuppressWarnings("deprecation")
    public void setExplicitTransformations(List<ContentTransformerRegistry.TransformationKey> explicitTransformations)
    {
        this.explicitTransformations = explicitTransformations;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName())
          .append("[ average=").append((long)averageTime).append("ms")
          .append("]");
        return sb.toString();
    }
    
    /**
     * Registers this instance with the {@link #setRegistry(ContentTransformerRegistry) registry}
     * if it is present.
     */
    public void register()
    {
        if (registry == null)
        {
            logger.warn("Property 'registry' has not been set.  Ignoring auto-registration: \n" +
                    "   transformer: " + this);
            return;
        }
        // first register any explicit transformations
        //if (explicitTransformations != null)
        //{
        //    for (ContentTransformerRegistry.TransformationKey key : explicitTransformations)
        // /   {
        //        registry.addExplicitTransformer(key, this);
        //    }
        //}
        // register this instance for the fallback case
        registry.addTransformer(this);
    }
    
    /**
     * Convenience to fetch and check the mimetype for the given content
     * 
     * @param content the reader/writer for the content
     * @return Returns the mimetype for the content
     * @throws AlfrescoRuntimeException if the content doesn't have a mimetype
     */
    protected String getMimetype(ContentAccessor content)
    {
        String mimetype = content.getMimetype();
        if (mimetype == null)
        {
            throw new AlfrescoRuntimeException("Mimetype is mandatory for transformation: " + content);
        }
        // done
        return mimetype;
    }
    
    /**
     * Added for backward compatibility of existing content transformers
     * 
     * @param sourceMimetype    the source mimetype
     * @param targetMimetype    the target mimetype
     * @return double           the reliability value of the content transformer ranging from 0 to 1
     */
    protected abstract double getReliability(String sourceMimetype, String targetMimetype);
    
    /**
     * Convenience method to check the reliability of a transformation
     * 
     * @param reader ContentReader
     * @param writer ContentWriter
     * @throws AlfrescoRuntimeException if the reliability isn't > 0
     */
    protected void checkReliability(ContentReader reader, ContentWriter writer)
    {
        String sourceMimetype = getMimetype(reader);
        String targetMimetype = getMimetype(writer);
        double reliability = getReliability(sourceMimetype, targetMimetype);
        if (reliability <= 0.0)
        {
            throw new AlfrescoRuntimeException("Zero scoring transformation attempted: \n" +
                    "   reader: " + reader + "\n" +
                    "   writer: " + writer);
        }
        // it all checks out OK
    }

    /**
     * {@inheritDoc}<p>
     * 
     * Implementation calls the deprecated overloaded method without the sourceSize parameter.
     * Note: source size checked has not been added to this deprecated class.
     */
    @Override
    public boolean isTransformable(String sourceMimetype, long sourceSize, String targetMimetype, TransformationOptions options)
    {
        return
            isTransformableMimetype(sourceMimetype, targetMimetype, options) &&
            isTransformableSize(sourceMimetype, sourceSize, targetMimetype, options);
    }
    
    /**
     * @deprecated use version with extra sourceSize parameter.
     */
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        ParameterCheck.mandatoryString("sourceMimetype", sourceMimetype);
        ParameterCheck.mandatoryString("targetMimetype", targetMimetype);
        
        double reliability = getReliability(sourceMimetype, targetMimetype);
        boolean result = true;
        if (reliability <= 0.0)
        {
            result = false;
        }
        return result;
    }

    /**
     * Checks the supplied mimetypes are supported by calling the deprecated
     * {@link #isTransformable(String, String, TransformationOptions)} method.
     */
    @Override
    public boolean isTransformableMimetype(String sourceMimetype, String targetMimetype,
            TransformationOptions options)
    {
        return isTransformable(sourceMimetype, targetMimetype, options);
    }

    /**
     * Always returns {@code true} to indicate size is not an issue.
     */
    @Override
    public boolean isTransformableSize(String sourceMimetype, long sourceSize,
            String targetMimetype, TransformationOptions options)
    {
        return true;
    }

    /**
     * Always returns {@code -1} to indicate an unlimited size.
     */
    @Override
    public long getMaxSourceSizeKBytes(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        return -1;
    }
    
    /**
     * @see org.alfresco.repo.content.transform.ContentTransformer#isTransformable(java.lang.String, java.lang.String, org.alfresco.service.cmr.repository.TransformationOptions)
     */
    @SuppressWarnings("deprecation")
    public boolean isExplicitTransformation(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
       boolean result = false;
       
       if (this.explicitTransformations != null)
       {
           for (ContentTransformerRegistry.TransformationKey transformationKey : this.explicitTransformations)
           {
               if (transformationKey.getSourceMimetype().equals(sourceMimetype) == true &&
                   transformationKey.getTargetMimetype().equals(targetMimetype) == true)
               {
                   result = true;
                   break;
               }
           }
       }
       
       return result;
    }
    
    /**
     * Method to be implemented by subclasses wishing to make use of the common infrastructural code
     * provided by this class.
     * 
     * @param reader the source of the content to transform
     * @param writer the target to which to write the transformed content
     * @param options a map of options to use when performing the transformation.  The map
     *      will never be null.
     * @throws Exception exceptions will be handled by this class - subclasses can throw anything
     */
    protected abstract void transformInternal(
            ContentReader reader,
            ContentWriter writer,
            Map<String, Object> options) throws Exception;
    
    /**
     * @see #transform(ContentReader, ContentWriter, Map)
     * @see #transformInternal(ContentReader, ContentWriter, Map)
     */
    public final void transform(ContentReader reader, ContentWriter writer) throws ContentIOException
    {
        Map<String, Object> optionsMap = null;
        transform(reader, writer, optionsMap);
    }
    
    /**
     * @see org.alfresco.repo.content.transform.ContentTransformer#transform(org.alfresco.service.cmr.repository.ContentReader, org.alfresco.service.cmr.repository.ContentWriter, org.alfresco.service.cmr.repository.TransformationOptions)
     */
    public final void transform(ContentReader reader, ContentWriter writer, TransformationOptions options)
        throws ContentIOException
    {
        Map<String, Object> optionsMap = options.toMap();
        transform(reader, writer, optionsMap);
    }

    /**
     * Performs the following:
     * <ul>
     *   <li>Times the transformation</li>
     *   <li>Ensures that the transformation is allowed</li>
     *   <li>Calls the subclass implementation of {@link #transformInternal(ContentReader, ContentWriter, Map)}</li>
     *   <li>Transforms any exceptions generated</li>
     *   <li>Logs a successful transformation</li>
     * </ul>
     * Subclass need only be concerned with performing the transformation.
     * <p>
     * If the options provided are null, then an empty map will be created.
     */
    @SuppressWarnings("deprecation")
    public final void transform(
            ContentReader reader,
            ContentWriter writer,
            Map<String, Object> options) throws ContentIOException
    {
        // begin timing
        long before = System.currentTimeMillis();
        
        // check options map
        if (options == null)
        {
            options = Collections.emptyMap();
        }
        
        try
        {
            // Check the reliability
            checkReliability(reader, writer);

            // Transform
            transformInternal(reader, writer, options);
        }
        catch (Throwable e)
        {
            // Make sure that this transformation gets set back i.t.o. time taken.
            // This will ensure that transformers that compete for the same transformation
            // will be prejudiced against transformers that tend to fail
            recordTime(10000);   // 10 seconds, i.e. rubbish
            
            throw new ContentIOException("Content conversion failed: \n" +
                    "   reader: " + reader + "\n" +
                    "   writer: " + writer + "\n" +
                    "   options: " + options,
                    e);
        }
        finally
        {
            // check that the reader and writer are both closed
            if (reader.isChannelOpen())
            {
                logger.error("Content reader not closed by transformer: \n" +
                        "   reader: " + reader + "\n" +
                        "   transformer: " + this);
            }
            if (writer.isChannelOpen())
            {
                logger.error("Content writer not closed by transformer: \n" +
                        "   writer: " + writer + "\n" +
                        "   transformer: " + this);
            }
        }
        
        // record time
        long after = System.currentTimeMillis();
        recordTime(after - before);
        
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Completed transformation: \n" +
                    "   reader: " + reader + "\n" +
                    "   writer: " + writer + "\n" +
                    "   options: " + options + "\n" +
                    "   transformer: " + this);
        }
    }

    /**
     * @return Returns the calculated running average of the current transformations
     */
    public synchronized long getTransformationTime()
    {
        return (long) averageTime;
    }

    public long getTransformationTime(String sourceMimetype, String targetMimetype)
    {
        return (long) averageTime;
    }

    /**
     * Records and updates the average transformation time for this transformer.
     * <p>
     * Subclasses should call this after every transformation in order to keep
     * the running average of the transformation times up to date.
     * <p>
     * This method is thread-safe.  The time spent in this method is negligible
     * so the impact will be minor.
     * 
     * @param transformationTime the time it took to perform the transformation.
     *      The value may be 0.
     */
    protected final synchronized void recordTime(long transformationTime)
    {
        if (count == Long.MAX_VALUE)
        {
            // we have reached the max count - reduce it by half
            // the average fluctuation won't be extreme
            count /= 2L;
        }
        // adjust the average
        count++;
        double diffTime = ((double) transformationTime) - averageTime;
        averageTime += diffTime / (double) count;
    }
    
    /**
     * Sets the Spring bean name.
     */
    @Override
    public void setBeanName(String beanName)
    {
        this.beanName = beanName;
    }

    /**
     * Returns the Spring bean name.
     */
    public String getBeanName()
    {
        return beanName;
    }
    
    /**
     * Returns transformer name. Uses the Spring bean name, but if null uses the class name.
     */
    public String getName()
    {
        return (beanName == null) ? getClass().getSimpleName() : beanName;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractContentTransformer other = (AbstractContentTransformer) obj;
        if (beanName == null)
        {
            if (other.beanName != null)
                return false;
        }
        else if (!beanName.equals(other.beanName))
            return false;
        return true;
    }
}
