package com.springbootbeginner.springbootfirst;

import com.springbootbeginner.springbootfirst.model.PanelResponse;
import com.springbootbeginner.springbootfirst.model.ResultModel;
import com.springbootbeginner.springbootfirst.model.ServiceResponse;
import com.springbootbeginner.springbootfirst.model.SmmPanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.*;

@Service
public class AppService {


    double exchangeRate = 83.06;
    List<ServiceResponse> serviceResponseList;
    List<PanelResponse> panelResponseList;
    List<ResultModel> comparisonList = new ArrayList<ResultModel>();
    String botToken = "5644977891:AAGYiDBwBeBNL3r5gSQ3JCmZQSULTm0DzhM";
    long chatId = -4003148302L;
    String parseMode = "HTML";

    private final RestTemplate restTemplate;

    @Autowired
    public AppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public void getComparedResult() throws IOException {

        serviceResponseList = getAppServices();

        if (!serviceResponseList.isEmpty()) {
            for (ServiceResponse serviceResponse : serviceResponseList) {
                int panelNum = serviceResponse.getServedByPanel();
                int panelServiceNum = serviceResponse.getSmmPanelServiceId();
                SmmPanel smmDetail = getPanelMap().get(panelNum);

                panelResponseList = performPanelApiCall(smmDetail.getBaseUrl(), smmDetail.getApiKey());

                for (PanelResponse panelResponse : panelResponseList) {
                    if (panelResponse.getService() == panelServiceNum) {
                        Double inrRate = panelResponse.getRate() * exchangeRate;
                        comparisonList.add(new ResultModel(serviceResponse.getId(), serviceResponse.getRate(),
                                inrRate, panelResponse.getService(), serviceResponse.getName()));
                    }
                }
            }
        }

        Iterator<ResultModel> iterator = comparisonList.iterator();
        List<ResultModel> batch = new ArrayList<>();
        while (iterator.hasNext()) {
            batch.add(iterator.next());
            if (batch.size() == 4 || (!iterator.hasNext() && !batch.isEmpty())) {
                StringBuilder text = new StringBuilder();
                for (ResultModel result : batch) {
                    String resultText = result.toString();
                    text.append(resultText);
                }
                sendMessage(botToken, chatId, text.toString(), parseMode);
                batch.clear();
            }
        }
    }


    public ArrayList<ServiceResponse> getAppServices() {
        try {
            String url = "https://igboosts.info/smm-insta/services/get-all-enabled-services";
            ResponseEntity<ArrayList<ServiceResponse>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ArrayList<ServiceResponse>>() {
                    }
            );
            ArrayList<ServiceResponse> appServicesList = responseEntity.getBody();
            if (appServicesList != null) {
                return appServicesList;
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.out.println("error caugth in getAppService");
            e.printStackTrace();
            return null;
        }
    }


    public ArrayList<PanelResponse> performPanelApiCall(String baseUrl, String apiKey) {
        try {
            String apiUrl = baseUrl + "?key=" + apiKey + "&action=services";
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ArrayList<PanelResponse> responseList = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ArrayList<PanelResponse>>() {
                    }
            ).getBody();

            if (responseList != null) {
                return responseList;
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    public void sendMessage(String botToken, long chatId, String text, String parseMode) throws IOException {
        try {
            String apiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("chat_id", String.valueOf(chatId));
            body.add("text", text);
            body.add("parse_mode", parseMode);
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
            } else {
                System.err.println("API Request failed with HTTP error code: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            System.out.println("error caugth in send message method");
            e.printStackTrace();
        }
    }


    public Map<Integer, SmmPanel> getPanelMap() {
        Map<Integer, SmmPanel> panelMap = new HashMap<>();
        panelMap.put(AppConstants.PANEL.HONEST_SMM_ANKUSH2015, new SmmPanel(AppConstants.PANEL.HONEST_SMM_BASE_URL,
                AppConstants.PANEL.HONEST_SMM_ANKUSH2015_API_KEY));
        panelMap.put(AppConstants.PANEL.REALSITE_ANKUSH2015,
                new SmmPanel(AppConstants.PANEL.REALSITE_BASE_URL, AppConstants.PANEL.REALSITE_ANKUSH2015_API_KEY));
        panelMap.put(AppConstants.PANEL.HONEST_SMM_ANKUSH2015TEST, new SmmPanel(
                AppConstants.PANEL.HONEST_SMM_BASE_URL, AppConstants.PANEL.HONEST_SMM_ANKUSH2015TEST_API_KEY));
        panelMap.put(AppConstants.PANEL.HONEST_SMM_TUBEBOOST, new SmmPanel(AppConstants.PANEL.HONEST_SMM_BASE_URL,
                AppConstants.PANEL.HONEST_SMM_TUBEBOOST_API_KEY));
        panelMap.put(AppConstants.PANEL.SMM_SURGE_TUBEBOOST, new SmmPanel(AppConstants.PANEL.SMM_SURGE_BASE_URL,
                AppConstants.PANEL.SMM_SURGE_TUBEBOOST_API_KEY));
        panelMap.put(AppConstants.PANEL.SMM_BIRLA_TUBEBOOST, new SmmPanel(AppConstants.PANEL.SMM_BIRLA_BASE_URL,
                AppConstants.PANEL.SMM_BIRLA_TUBEBOOST_API_KEY));
        panelMap.put(AppConstants.PANEL.SMM_OWL_TUBEBOOST,
                new SmmPanel(AppConstants.PANEL.SMM_OWL_BASE_URL, AppConstants.PANEL.SMM_OWL_TUBEBOOST_API_KEY));
        return panelMap;
    }


}