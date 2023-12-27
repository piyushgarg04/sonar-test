package com.xpo.ltl.shipment.service.util;

//import java.util.EnumMap;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

public class FBDSEngToFrenchTranslatorUtil {
	private static final HashMap<String, String> translatorMap = createMap(); 
	public static HashMap<String, String> createMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("Delivery Receipt", "Bon de livraison");
		map.put("Contractual Copy", "Copie contractuelle");
		map.put("Pro Number", "Pro Numéro");
		map.put("Equip Number", "Numéro d'équipement");
		map.put("Date", "Date");
		map.put("Origin", "Origine");
		map.put("Destination", "Destination");
		map.put("Our Revenue", "Nos revenus");
		map.put("Advance", "Avance");
		map.put("Beyond", "Au-delà");
		map.put("Route", "Itinéraire");
		map.put("Consignee", "Destinataire");
		map.put("Shipper", "Expéditeur");
		map.put("Bill To", "Facturer à");
		map.put("Appointment", "Rendez-vous");
		map.put("Shipper Numbers", "Numéros d'expéditeur");
		map.put("Description Of Articles And Remarks", "Description des articles et remarques");
		map.put("Weight", "Poids");
		map.put("Rate", "Taux");
		map.put("Charges", "Des charges");
		map.put("Received", "Reçu");
		map.put("Shrink Wrap Intact?", "Emballage Intact?");
		map.put("Delivered", "Livré");
		map.put("Time", "Heure");
		map.put("Driver Signature", "Signature du conducteur");
		map.put("Inside Delivery", "Livraison à l'intérieur");
		map.put("Liftgate Service", "Service de hayon");
		map.put("Residential Delivery", "Livraison résidentielle");
		map.put("Construction Util.", "Utilitaire de construction");
		map.put("Consignee Signature", "Signature du destinataire");
		map.put("Print Consignee Name", "Imprimer le nom du destinataire");
		map.put("Customer Copy", "Copie du client");
		map.put("Pcs", "PCS");
		map.put("Hm", "HM");
		return map;
	}
	public String getFrenchTranslation(String key) {
		if(translatorMap.containsKey(key)) {			
			return translatorMap.get(key);
		}
		return StringUtils.EMPTY;
	}
}