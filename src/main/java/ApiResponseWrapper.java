import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResponseWrapper {

    @JsonProperty("label")
    private ApiFileResponse apiFileResponse;

    public ApiFileResponse getApiFileResponse() {
        return apiFileResponse;
    }

    public void setApiFileResponse(ApiFileResponse apiFileResponse) {
        this.apiFileResponse = apiFileResponse;
    }
}
