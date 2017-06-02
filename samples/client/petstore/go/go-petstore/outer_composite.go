/* 
 * Swagger Petstore
 *
 * This spec is mainly for testing Petstore server and contains fake endpoints, models. Please do not use this for any other purpose. Special characters: \" \\
 *
 * OpenAPI spec version: 1.0.0
 * Contact: apiteam@swagger.io
 * Generated by: https://github.com/swagger-api/swagger-codegen.git
 */

package petstore

type OuterComposite struct {

	MyNumber *OuterNumber `json:"my_number,omitempty"`

	MyString *OuterString `json:"my_string,omitempty"`

	MyBoolean *OuterBoolean `json:"my_boolean,omitempty"`
}
