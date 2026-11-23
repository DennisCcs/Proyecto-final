package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.enums.TipoDocumento;
import pe.edu.upeu.sysvstarbucks.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteRepository {
    public List<Cliente> findAll() {
        String sql = "SELECT numero_documento, tipo_documento, nombres, direccion FROM cliente ORDER BY nombres";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) clientes.add(map(result));
            return clientes;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los clientes desde H2.", e);
        }
    }

    public Cliente save(Cliente cliente) {
        String sql = "INSERT INTO cliente (numero_documento, tipo_documento, nombres, direccion) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParametros(statement, cliente);
            statement.executeUpdate();
            return cliente;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el cliente en H2.", e);
        }
    }

    public Cliente update(Cliente cliente, String documentoAnterior) {
        String sql = "UPDATE cliente SET numero_documento = ?, tipo_documento = ?, nombres = ?, direccion = ? WHERE numero_documento = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParametros(statement, cliente);
            statement.setString(5, documentoAnterior);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el cliente que se quiere actualizar.");
            }
            return cliente;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el cliente en H2.", e);
        }
    }

    public void deleteByDocumento(String documento) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM cliente WHERE numero_documento = ?")) {
            statement.setString(1, documento);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el cliente que se quiere eliminar.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar el cliente. Puede estar relacionado con una venta.", e);
        }
    }

    private void setParametros(PreparedStatement statement, Cliente cliente) throws SQLException {
        statement.setString(1, cliente.getNumeroDocumento());
        statement.setString(2, cliente.getTipoDocumento().name());
        statement.setString(3, cliente.getNombres());
        statement.setString(4, cliente.getDireccion());
    }

    private Cliente map(ResultSet result) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setNumeroDocumento(result.getString("numero_documento"));
        cliente.setTipoDocumento(TipoDocumento.valueOf(result.getString("tipo_documento")));
        cliente.setNombres(result.getString("nombres"));
        cliente.setDireccion(result.getString("direccion"));
        return cliente;
    }
}
