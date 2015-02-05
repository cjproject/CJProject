package com.acme.customization.shared;


public class ProjectGlobalsEInv
{	
	private static String m_ProjectGUID = "{5C46B2D6-F40E-0B33-AE4C-34F4673F298E}";
	
	public static final int MAPPING_TYPE_RECEIVED = 0;
	public static final int MAPPING_TYPE_SENDED = 1;
	
	public static final int ITEM_MAPPING_UBL_FIELD_CODE = 1;
	public static final int ITEM_MAPPING_UBL_FIELD_DESCRIPTION = 2;
	public static final int ITEM_MAPPING_UBL_FIELD_BUYER = 3;
	public static final int ITEM_MAPPING_UBL_FIELD_SELLER = 4;
	public static final int ITEM_MAPPING_UBL_FIELD_MANUFACTURER = 5;
	public static final int ITEM_MAPPING_UBL_FIELD_BRAND = 6;
	public static final int ITEM_MAPPING_UBL_FIELD_MODEL = 7;
	public static final int ITEM_MAPPING_UBL_FIELD_NOTE = 8;
	
	public static final int ITEM_MAPPING_SEARCH_FIELD_CODE = 1;
	public static final int ITEM_MAPPING_SEARCH_FIELD_DESCRIPTION = 2;
	public static final int ITEM_MAPPING_SEARCH_FIELD_PRODUCER = 3;
	public static final int ITEM_MAPPING_SEARCH_FIELD_ITEMARPCODE = 4;
	public static final int ITEM_MAPPING_SEARCH_FIELD_BARCODE = 5;
	public static final int ITEM_MAPPING_SEARCH_FIELD_GTIPCODE = 6;
	public static final int ITEM_MAPPING_SEARCH_FIELD_BRAND = 7;
	public static final int ITEM_MAPPING_SEARCH_FIELD_AUXCODE1 = 8;
	public static final int ITEM_MAPPING_SEARCH_FIELD_AUXCODE2 = 9;
	public static final int ITEM_MAPPING_SEARCH_FIELD_AUXCODE3 = 10;
	public static final int ITEM_MAPPING_SEARCH_FIELD_AUXCODE4 = 11;
	public static final int ITEM_MAPPING_SEARCH_FIELD_AUXCODE5 = 12;
	
	
	
	
	public static final int EINV_STATUS_GIBE_GONDERILECEK = 0;
	public static final int EINV_STATUS_MUHURDE_ONAYDA = 1;
	public static final int EINV_STATUS_MUHURLENDI_ONAYLANDI = 2;
	public static final int EINV_STATUS_ZARFLANDI_PAKETLENDI = 3;
	public static final int EINV_STATUS_GIBE_GONDERILDI = 4;
	public static final int EINV_STATUS_GIBE_GONDERILEMEDI = 5;
	public static final int EINV_STATUS_GIBDE_ISLENDI_ALICIYA_ILETILECEK = 6;
	public static final int EINV_STATUS_GIBDE_ISLENEMEDI = 7;
	public static final int EINV_STATUS_ALICIYA_GONDERILDI = 8;
	public static final int EINV_STATUS_ALICIYA_GONDERILEMEDI = 9;
	public static final int EINV_STATUS_ALICIDA_ISLENDI_BASARIYLA_TAMAMLANDI = 10;
	public static final int EINV_STATUS_ALICIDA_ISLENEMEDI = 11;
	public static final int EINV_STATUS_KABUL_EDILDI = 12;
	public static final int EINV_STATUS_RED_EDILDI = 13;
	public static final int EINV_STATUS_IADE_EDILDI = 14;
	public static final int EINV_STATUS_KABUL_EDILDI_UYGULAMA_YANITI_OLUÞTURULMADI = 15;
	public static final int EINV_STATUS_RED_EDILDI_UYGULAMA_YANITI_OLUÞTURULMADI = 16;
	public static final int EINV_STATUS_ALINDI = 17;
	public static final int EINV_STATUS_SUNUCUDA_ISLENDI = 18;
	public static final int EINV_STATUS_SUNUCUDA_MUHURLENDI = 19;
	public static final int EINV_STATUS_SUNUCUDA_ZARFLANDI = 20;
	public static final int EINV_STATUS_SUNUCUDA_HATA_ALINDI = 21;
	public static final int EINV_STATUS_SUNUCUYA_GONDERILDI = 22;
	public static final int EINV_STATUS_SILINDI = 50;
	public static final int EINV_STATUS_ARSIVE_GONDERILDI = 60;
	public static final int EINV_STATUS_SISTEM_YANITI = 100;
	public static final int EINV_STATUS_UYGULAMA_YANITI = 101;
	
	
	
	
	public static final int PR_ACCEPT = 1;
	public static final int PR_REJECT = 2;
	
	public static final int PROFILE_ID_BASIC = 1;
	public static final int PROFILE_ID_COMMERCIAL = 2;
	
	public static final int OPTYPE_SEND = 0;
	public static final int OPTYPE_RECEIVE = 1;
	
	public static final int RECTYPE_SENDED_INV = 110;
	public static final int RECTYPE_RECEIVED_INV = 111;
	public static final int RECTYPE_RECEIVED_RET_INV = 113;
	public static final int RECTYPE_SENDED_PR = 114;
	public static final int RECTYPE_RECEIVED_PR = 115;
	public static final int RECTYPE_PACKED_FOR_SENDING_PR = 116;
	public static final int RECTYPE_PACKED_FOR_SENDING_EINV = 117;
	public static final int RECTYPE_RECEIVED_SR = 119;
	
	public static final int ENVELOPE_TYPE_SENDER = 0;
	public static final int ENVELOPE_TYPE_SYSTEM = 1;
	public static final int ENVELOPE_TYPE_POSTBOX = 2;
	
	public static final int STATUS_WILL_PACK_OR_SAVE = 3;
	public static final int STATUS_PACKED_OR_SAVED = 5;
	public static final int STATUS_SENT = 7;
	public static final int STATUS_ARCHIVED = 8;
	public static final int STATUS_DENIED = 10;
	
	public static final int TRANSACTION_STATUS_WILL_BE_SEND = 0;
	public static final int TRANSACTION_STATUS_SENT = 1;
	
	public static final int TRANSACTION_TYPE_APPRESP = 1;
	public static final int TRANSACTION_TYPE_EINV = 2;
	
	public static final int OPTYPE_OUTGOING = 0;
	public static final int OPTYPE_INCOMING = 1;
	
	public static final int MAPPING_CARD_TYPE_ITEM = 0;
	public static final int MAPPING_CARD_TYPE_PROMOTION = 1;
	public static final int MAPPING_CARD_TYPE_DISCOUNT_SALES = 2;
	public static final int MAPPING_CARD_TYPE_EXPENSE_SALES = 3;
	public static final int MAPPING_CARD_TYPE_SERVICE_SALES = 4;
	public static final int MAPPING_CARD_TYPE_DEPOSIT = 5;
	public static final int MAPPING_CARD_TYPE_FIXEDASSET = 8;
	public static final int MAPPING_CARD_TYPE_ITEMCLASS = 10;
	public static final int MAPPING_CARD_TYPE_SET = 16;
	public static final int MAPPING_CARD_TYPE_SETITEM = 17;
	public static final int MAPPING_CARD_TYPE_DISCOUNT_PURCH = 22;
	public static final int MAPPING_CARD_TYPE_EXPENSE_PURCH = 33;
	public static final int MAPPING_CARD_TYPE_SERVICE_PURCH = 44;
	
	

	public static String getM_ProjectGUID()
	
	{
		return m_ProjectGUID;
	}
	public static void setM_ProjectGUID(String projectGUID)
	{
		m_ProjectGUID = projectGUID;

	}	
	
}