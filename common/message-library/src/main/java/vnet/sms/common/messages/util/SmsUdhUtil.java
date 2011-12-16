/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is "SMS Library for the Java platform".
 *
 * The Initial Developer of the Original Code is Markus Eriksson.
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package vnet.sms.common.messages.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import vnet.sms.common.messages.DataCodingScheme;
import vnet.sms.common.messages.SmsConstants;
import vnet.sms.common.messages.UdhElement;
import vnet.sms.common.messages.UserData;

/**
 * Toolkit class for UdhElement objects.
 * 
 * @author Markus Eriksson
 * @version $Id: SmsUdhUtil.java 410 2006-03-13 19:48:31Z c95men $
 */
public final class SmsUdhUtil {
	/**
	 * Constructor for SmsUdhUtil.
	 */
	private SmsUdhUtil() {
	}

	/**
	 * Calculates the number of bytes needed for the supplied udh elements.
	 * 
	 * @param udhElements
	 *            The udh elements
	 * @return The size (in bytes)
	 */
	public static int getTotalSize(final UdhElement[] udhElements) {
		int totLength = 0;

		if (udhElements == null) {
			return 0;
		}

		for (final UdhElement udhElement : udhElements) {
			totLength += udhElement.getTotalSize();
		}

		return totLength;
	}

	/**
	 * Returns the whole udh as a byte array.
	 * <p>
	 * The returned UDH is the same as specified when the message was created.
	 * No concat headers are added.
	 * 
	 * TODO: Rename this function. The name is totally wrong.
	 * 
	 * @return the UDH elements as a byte array.
	 */
	public static byte[] toByteArray(final UdhElement[] udhElements) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(100);

		if (udhElements == null) {
			return new byte[0];
		}

		baos.write((byte) SmsUdhUtil.getTotalSize(udhElements));

		try {
			for (final UdhElement udhElement : udhElements) {
				udhElement.writeTo(baos);
			}
		} catch (final IOException ioe) {
			throw new RuntimeException(ioe);
		}

		return baos.toByteArray();
	}

	/**
	 * Calculates if the given data needs a concatenated SMS.
	 * 
	 * @param ud
	 *            User data
	 * @param udh
	 *            UDH elements
	 * @return true if the message must be concatentated.
	 */
	public static boolean isConcat(final UserData ud, final byte[] udh) {
		final int udLength = ud.getLength();

		int bytesLeft = 140;
		int maxChars;

		if (udh != null) {
			bytesLeft -= udh.length;
		}

		switch (ud.getDcs().getAlphabet()) {
		case DataCodingScheme.ALPHABET_GSM:
			maxChars = (bytesLeft * 8) / 7;
			break;

		case DataCodingScheme.ALPHABET_UCS2:
			maxChars = bytesLeft / 2;
			break;

		case DataCodingScheme.ALPHABET_8BIT:
		default:
			maxChars = bytesLeft;
			break;
		}

		return (udLength > maxChars);
	}

	/**
	 * Creates a "8Bit concatenated" UDH element using
	 * UDH_IEI_CONCATENATED_8BIT.
	 * 
	 * This can be used to create a concatenated SMS.
	 * 
	 * @param refNr
	 *            The reference number of this SMS, must be the same in all SMS.
	 *            Max 255.
	 * @param totalNumberOfSms
	 *            Total number of SMS. Max 255.
	 * @param seqNr
	 *            Sequence number. Max 255.
	 * @return A UdhElement
	 */
	public static UdhElement get8BitConcatUdh(final int refNr,
	        final int totalNumberOfSms, final int seqNr) {
		final byte[] udh = new byte[3];

		udh[0] = (byte) (refNr & 0xff);
		udh[1] = (byte) (totalNumberOfSms & 0xff);
		udh[2] = (byte) (seqNr & 0xff);

		return new UdhElement(SmsConstants.UDH_IEI_CONCATENATED_8BIT, udh);
	}

	/**
	 * Creates a "Message waiting" UDH element using UDH_IEI_SPECIAL_MESSAGE.
	 * <p>
	 * If more than one type of message is required to be indicated within one
	 * SMS message, then multiple "Message waiting" UDH elements must be used.
	 * <p>
	 * <b>Special handling in concatenated messages:</b><br>
	 * <i> "In the case where this IEI is to be used in a concatenated SM then
	 * the IEI, its associated IEI length and IEI data shall be contained in the
	 * first segment of the concatenated SM. The IEI, its associated IEI length
	 * and IEI data should also be contained in every subsequent segment of the
	 * concatenated SM although this is not mandatory. However, in the case
	 * where these elements are not contained in every subsequent segment of the
	 * concatenated SM and where an out of sequence segment delivery occurs or
	 * where the first segment is not delivered then processing difficulties may
	 * arise at the receiving entity which may result in the concatenated SM
	 * being totally or partially discarded." </i>
	 * 
	 * @param storeMsg
	 *            Set to true if the message should be stored
	 * @param msgType
	 *            Message type, may be one of MESSAGE_WAITING_VOICE,
	 *            MESSAGE_WAITING_FAX, MESSAGE_WAITING_EMAIL or
	 *            MESSAGE_WAITING_OTHER.
	 * @param msgCount
	 *            Number of messages waiting for retrieval. Max 255 messages.
	 *            The value 255 shall be taken to mean 255 or greater.
	 * @return A UdhElement
	 */
	public static UdhElement getMessageWaitingUdh(final boolean storeMsg,
	        final int msgType, final int msgCount) {
		final byte[] udh = new byte[2];

		udh[0] = (byte) (msgType & 0x7f);
		if (storeMsg) {
			udh[0] |= (byte) (0x80);
		}
		udh[1] = (byte) (msgCount & 0xff);

		return new UdhElement(SmsConstants.UDH_IEI_SPECIAL_MESSAGE, udh);
	}

	/**
	 * Creates a "8 bit Application Port Adressing" UDH element using
	 * UDH_IEI_APP_PORT_8BIT
	 * <p>
	 * Note! Only values between 240 and 255 are usable, the rest of the port
	 * numbers are marked as reserved.
	 * <p>
	 * <b>Special handling in concatenated messages:</b><br>
	 * <i> In the case where this IE is to be used in a concatenated SM then the
	 * IEI, its associated IEI length and IEI data shall be contained in the
	 * first segment of the concatenated SM. The IEI, its associated IEI length
	 * and IEI data shall also be contained in every subsequent segment of the
	 * concatenated SM. </i>
	 * 
	 * @param destPort
	 *            Destination port
	 * @param origPort
	 *            Source port
	 * @return A UdhElement
	 */
	public static UdhElement get8BitApplicationPortUdh(final int destPort,
	        final int origPort) {
		final byte[] udh = new byte[2];

		udh[0] = (byte) (destPort & 0xff);
		udh[1] = (byte) (origPort & 0xff);

		return new UdhElement(SmsConstants.UDH_IEI_APP_PORT_8BIT, udh);
	}

	/**
	 * Creates a "16 bit Application Port Adressing" UDH element using
	 * UDH_IEI_APP_PORT_16BIT
	 * <p>
	 * Note! Only values between 0 and 16999 are usable, the rest of the port
	 * numbers are marked as reserved.
	 * <p>
	 * <b>Special handling in concatenated messages:</b><br>
	 * <i> In the case where this IE is to be used in a concatenated SM then the
	 * IEI, its associated IEI length and IEI data shall be contained in the
	 * first segment of the concatenated SM. The IEI, its associated IEI length
	 * and IEI data shall also be contained in every subsequent segment of the
	 * concatenated SM. </i>
	 * 
	 * @param destPort
	 *            Destination port
	 * @param origPort
	 *            Source port
	 * @return A UdhElement
	 */
	public static UdhElement get16BitApplicationPortUdh(final int destPort,
	        final int origPort) {
		final byte[] udh = new byte[4];

		udh[0] = (byte) ((destPort >> 8) & 0xff);
		udh[1] = (byte) (destPort & 0xff);
		udh[2] = (byte) ((origPort >> 8) & 0xff);
		udh[3] = (byte) (origPort & 0xff);

		return new UdhElement(SmsConstants.UDH_IEI_APP_PORT_16BIT, udh);
	}

	/**
	 * Creates a "16Bit concatenated" UDH element using
	 * UDH_IEI_CONCATENATED_16BIT
	 * <p>
	 * This can be used to create a concatenated SMS.
	 * 
	 * @param refNr
	 *            The reference number of this SMS, must be the same in all SMS.
	 *            Max 65536
	 * @param totalNumberOfSms
	 *            Total number of SMS. Max 255
	 * @param seqNr
	 *            Sequence number. Max 255
	 * @return A UdhElement
	 */
	public static UdhElement get16BitConcatUdh(final int refNr,
	        final int totalNumberOfSms, final int seqNr) {
		final byte[] udh = new byte[4];

		udh[0] = (byte) ((refNr >> 8) & 0xff);
		udh[1] = (byte) (refNr & 0xff);
		udh[2] = (byte) (totalNumberOfSms & 0xff);
		udh[3] = (byte) (seqNr & 0xff);

		return new UdhElement(SmsConstants.UDH_IEI_CONCATENATED_16BIT, udh);
	}

	/**
	 * Creates a "EMS Text Formatting" UDH element.
	 * 
	 * @param startPos
	 *            Start position of the text formatting. This position is
	 *            relative to the start of the UD field of the PDU.
	 * @param formatLen
	 *            The number of character to format. If 0 it sets the default
	 *            text formatting.
	 * @param alignment
	 *            Can be any of EMS_TEXT_ALIGN_*
	 * @param fontSize
	 *            Can be any of EMS_TEXT_SIZE_*
	 * @param style
	 *            Can be any of EMS_TEXT_STYLE_*
	 * @param foregroundColor
	 *            Can be any of EMS_TEXT_COLOR_*
	 * @param backgroundColor
	 *            Can be any of EMS_TEXT_COLOR_*
	 * @return A UdhElement
	 */
	public static UdhElement getEmsTextFormattingUdh(final int startPos,
	        final int formatLen, final byte alignment, final byte fontSize,
	        final byte style, final byte foregroundColor,
	        final byte backgroundColor) {
		final byte[] udh = new byte[4];

		udh[0] = (byte) (startPos & 0xff);
		udh[1] = (byte) (formatLen & 0xff);
		udh[2] = (byte) (((alignment & 0x03) | (fontSize & 0x0C) | (style & 0xF0)) & 0xff);
		udh[3] = (byte) (((foregroundColor & 0x0f) | (((backgroundColor & 0x0f) << 4) & 0xf0)) & 0xff);

		return new UdhElement(SmsConstants.UDH_IEI_EMS_TEXT_FORMATTING, udh);
	}

	/**
	 * Creates an ems user defined sound udh.
	 * 
	 * @param iMelodyData
	 *            The imelody data
	 * @param position
	 *            The position
	 * @return An UdhElement with a user defined sound
	 */
	public static UdhElement getEmsUserDefinedSoundUdh(
	        final byte[] iMelodyData, final int position) {
		final int iMelodyLength = iMelodyData.length;
		final byte[] udh = new byte[iMelodyLength + 1];
		udh[0] = (byte) position;
		System.arraycopy(iMelodyData, 0, udh, 1, iMelodyLength);

		return new UdhElement(SmsConstants.UDH_IEI_EMS_USER_DEFINED_SOUND, udh);
	}

	/**
	 * Creates an ems user prompt indicator udh.
	 * 
	 * @param numFragments
	 *            Number of fragments
	 * @return An UdhElement with an user prompt indicator
	 */
	public static UdhElement getEmsUserPromptIndicatorUdh(final int numFragments) {
		final byte[] udh = new byte[1];
		udh[0] = (byte) numFragments;

		return new UdhElement(SmsConstants.UDH_IEI_EMS_USER_PROMPT, udh);
	}

	/**
	 * Creates an ems variable picture udh.
	 * 
	 * @param bitmap
	 *            The bitmap data
	 * @param width
	 *            The width of the bitmap (in pixels)
	 * @param height
	 *            The height of the bitmap (in pixels)
	 * @param position
	 *            The position of the bitmap
	 * @return An UdhElement with the bitmap
	 */
	public static UdhElement getEmsVariablePictureUdh(final byte[] bitmap,
	        final int width, final int height, final int position) {
		final int otaBitmapLength = bitmap.length;
		final byte[] udh = new byte[otaBitmapLength + 3];

		udh[0] = (byte) position;
		udh[1] = (byte) (width / 8);
		udh[2] = (byte) height;

		System.arraycopy(bitmap, 0, udh, 3, otaBitmapLength);

		return new UdhElement(SmsConstants.UDH_IEI_EMS_VARIABLE_PICTURE, udh);
	}
}
