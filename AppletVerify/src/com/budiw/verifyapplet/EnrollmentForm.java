package com.budiw.verifyapplet;


import java.io.File;

import com.digitalpersona.onetouch.*;
import com.digitalpersona.onetouch.processing.*;
import javax.swing.JOptionPane;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

public class EnrollmentForm extends CaptureForm
{
	private static final long serialVersionUID = 7675828998942686645L;
	private DPFPEnrollment enroller = DPFPGlobal.getEnrollmentFactory().createEnrollment();
	public static final String DEFAULT_TEMPLATE_NAME = "template";
	private final String templateName, templatePath;
	
	public EnrollmentForm(String templateName) {
		super();
		if(templateName == "" || templateName == null)
			this.templateName = DEFAULT_TEMPLATE_NAME;
		else
			this.templateName = templateName;
		this.templatePath = System.getProperty("user.home") + "\\" + this.templateName + ".fpt";
	}
	
	protected void init()
	{
		super.init();
		this.setTitle("Fingerprint Enrollment");
		updateStatus();
	}

	protected void process(DPFPSample sample) {
		super.process(sample);
		// Process the sample and create a feature set for the enrollment purpose.
		DPFPFeatureSet features = extractFeatures(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);

		// Check quality of the sample and add to enroller if it's good
		if (features != null) try
		{
			makeReport("The fingerprint feature set was created.");
			enroller.addFeatures(features);		// Add feature set to template.
		}
		catch (DPFPImageQualityException ex) { }
		finally {
			updateStatus();

			// Check if template has been created.
			switch(enroller.getTemplateStatus())
			{
				case TEMPLATE_STATUS_READY:	// report success and stop capturing
					stop();
					writeFile(templatePath, enroller.getTemplate().serialize());
					uploadTemplate();
					setVisible(false);
					break;

				case TEMPLATE_STATUS_FAILED:	// report failure and restart capturing
					enroller.clear();
					stop();
					updateStatus();
					JOptionPane.showMessageDialog(EnrollmentForm.this, "The fingerprint template is not valid. Repeat fingerprint enrollment.", "Fingerprint Enrollment", JOptionPane.ERROR_MESSAGE);
					start();
					break;
			}
		}
	}
	
	private void updateStatus()
	{
		// Show number of samples needed.
		setStatus(String.format("Fingerprint samples needed: %1$s", enroller.getFeaturesNeeded()));
	}
	
	private void uploadTemplate() {
		HttpClient httpclient = new DefaultHttpClient();
		httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

		HttpPost httppost = new HttpPost("https://localhost/upload.php");
		File file = new File(templatePath);

		//This will appear on $_FILES
		MultipartEntity mpEntity = new MultipartEntity();
		ContentBody cbFile = new FileBody(file, "image/jpeg");
		mpEntity.addPart("userfile", cbFile);

		httppost.setEntity(mpEntity);
		//		System.out.println("executing request " + httppost.getRequestLine());

		try {
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity resEntity = response.getEntity();
			
//			System.out.println(response.getStatusLine());
			
			//Print content of response, check for verification message
			if (resEntity != null) {
				String responsePayload = EntityUtils.toString(resEntity);
				httpclient.getConnectionManager().shutdown();
				System.out.println(responsePayload);
			} 
			if (resEntity != null) {
				EntityUtils.consume(resEntity);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		httpclient.getConnectionManager().shutdown();
	}
	
}
