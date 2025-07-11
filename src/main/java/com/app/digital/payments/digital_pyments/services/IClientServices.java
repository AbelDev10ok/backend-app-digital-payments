package com.app.digital.payments.digital_pyments.services;

import java.util.List;

import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.ClientRequestDto;


public interface IClientServices {

    public ClientDto createClient(ClientRequestDto request);
    
    public List<ClientDto> getAllClients();
    
    public ClientDto getClientById(Long id);
    
    ClientDto updateClient(Long id, ClientRequestDto request);
    
    public void deletedClient(Long id);

}
