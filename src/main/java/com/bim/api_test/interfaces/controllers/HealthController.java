package com.bim.api_test.interfaces.controllers;
import com.bim.api_test.interfaces.dtos.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Verificação do estado de saúde da aplicação", description = "Verificação do estado de saúde da aplicação")
public class HealthController {

    @Value("${api.version}")
    public String apiVersion;


    @GetMapping("/health")
    @Operation(summary = "Api para verificar o estado de saúde da aplicação", description = "Api para verificar o estado de saúde da aplicação")
    public ResponseEntity<HealthResponse> health(){
        HealthResponse healthResponse = new HealthResponse("UP", apiVersion, "Connected");
        return ResponseEntity.ok(healthResponse);
    }
}
