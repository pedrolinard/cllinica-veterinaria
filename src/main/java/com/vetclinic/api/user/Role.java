package com.vetclinic.api.user;

/**
 * Papéis (roles) da equipe da clínica.
 * ADMIN: acesso total, incluindo gestão de usuários e serviços.
 * VET: conduz consultas e registra prontuários.
 * RECEPTIONIST: cadastra clientes/pets e agenda consultas.
 */
public enum Role {
    ADMIN,
    VET,
    RECEPTIONIST
}
