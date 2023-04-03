import {
  IoTClient,
  CreatePolicyCommand,
  AttachPolicyCommand,
  UpdateCertificateCommand,
} from "@aws-sdk/client-iot";
const REGION = "us-east-1";

const iotClient = new IoTClient({ region: REGION });

export const handler = async (event) => {
  const accountId = event.awsAccountId.toString().trim();
  const certificateId = event.certificateId.toString().trim();

  console.log(`CertificateId: ${certificateId}`);

  const certificateARN = `arn:aws:iot:${REGION}:${accountId}:cert/${certificateId}`;
  const policyName = `Policy_${certificateId}`;

  const policy = {
    Version: "2012-10-17",
    Statement: [
      {
        Effect: "Allow",
        Action: ["iot:Connect", "iot:Publish", "iot:Receive", "iot:Subscribe"],
        Resource: ["*"],
      },
    ],
  };

  const Policycommand = new CreatePolicyCommand({
    policyDocument: JSON.stringify(policy),
    policyName: policyName,
  });

  try {
    await iotClient.send(Policycommand);
  } catch (error) {
    if (error.name === "ResourceAlreadyExistsException") {
      console.warn("The policy already exists. Skipping policy creation.");
    } else {
      throw error;
    }
  }

  const AttachPolicy = new AttachPolicyCommand({
    policyName: policyName,
    target: certificateARN,
  });

  try {
    await iotClient.send(AttachPolicy);
  } catch (error) {
    if (error.name === "ResourceAlreadyExistsException") {
      console.warn("The policy is already attached. Skipping policy attachment.");
    } else {
      throw error;
    }
  }

  const UpdateCert = new UpdateCertificateCommand({
    certificateId: certificateId,
    newStatus: "ACTIVE",
  });

  try {
    await iotClient.send(UpdateCert);
  } catch (error) {
    if (error.name === "ResourceAlreadyExistsException") {
      console.warn("The certificate is already active. Skipping certificate update.");
    } else {
      throw error;
    }
  }
};
