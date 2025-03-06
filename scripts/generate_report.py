import mysql.connector
from mysql.connector import Error

# Database connection details
DB_HOST = "dig-buildertest.rtegroup.ie"
DB_PORT = "3306"
DB_NAME = "djbuilder"
DB_USER = "djbuilder"
DB_PASSWORD = 'vn&{gBFI>2v@GD#<@bkidicQPc`;0{k!Y]rFA`]<z~G5T(b~91cl,<Acs|M>cRs97,'

# Output file for the HTML report
OUTPUT_FILE = "/var/lib/jenkins/deployments/scripts/deployment_report.html"

def generate_report():
    """Connects to the database and generates an HTML report of deployment records."""
    try:
        # Connect to the database
        connection = mysql.connector.connect(
            host=DB_HOST,
            user=DB_USER,
            password=DB_PASSWORD,
            database=DB_NAME
        )

        if connection.is_connected():
            cursor = connection.cursor()
            cursor.execute("""
                SELECT id, application, branch, deployer, status, target_server, settings_file, timestamp, redeploy, approver, build_id
                FROM jenkins_deployments
                ORDER BY timestamp DESC
            """)
            rows = cursor.fetchall()

            # Generate the HTML report
            with open(OUTPUT_FILE, 'w') as f:
                f.write("""\
<html>
<head>
    <meta charset="UTF-8">
    <title>Deployment Records</title>
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css">
    <style>
        body { font-family: Arial, sans-serif; background-color: #f9f9f9; margin: 0; padding: 20px; }
        h3 { text-align: center; font-size: 26px; margin-bottom: 30px; }
        table { width: 90%; margin: 0 auto; border-collapse: collapse; background-color: #fffdd0; }
        th { background-color: #f2c94c; color: #333; font-weight: bold; text-align: left; padding: 12px; font-size: 20px; }
        td { text-align: left; padding: 12px; font-size: 18px; }
        tr:hover { background-color: #faf5d7; }
        .button { background-color: #4CAF50; border: none; color: white; padding: 10px 20px; text-align: center; text-decoration: none; display: inline-block; font-size: 16px; margin: 4px 2px; cursor: pointer; }
    </style>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
    <script>
    $(document).ready(function() {
        $('#deploymentTable').DataTable({
            paging: true,
            searching: true,
            ordering: true,
            pageLength: 10,
            lengthMenu: [10, 25, 50, 100],
            scrollY: '400px',
            scrollCollapse: true,
            info: true,
            order: [[7, 'desc']]  // Sort by timestamp in descending order
        });
    });
    </script>
</head>
<body>
    <h3>Deployment Records</h3>
    <table id="deploymentTable" class="display" border="1">
        <thead>
            <tr>
                <th>ID</th>
                <th>Application</th>
                <th>Branch</th>
                <th>Deployer</th>
                <th>Status</th>
                <th>Target Server</th>
                <th>Settings File</th>
                <th>Timestamp</th>
                <th>Redeploy</th>
                <th>Approver</th>  <!-- Add Approver Column Here -->
                <th>Build ID</th>
            </tr>
        </thead>
        <tbody>
""")

                # Write the data rows to the HTML file
                for row in rows:
                    id, application, branch, deployer, status, target_server, settings_file, timestamp, redeploy, approver, build_id = row
                    f.write(f"""
                <tr>
                    <td>{id}</td>
                    <td>{application}</td>
                    <td>{branch}</td>
                    <td>{deployer}</td>
                    <td>{status}</td>
                    <td>{target_server}</td>
                    <td>{settings_file}</td>
                    <td>{timestamp}</td>
                    <td><a href="{redeploy}" class="button">Redeploy</a></td>
                    <td>{approver}</td>
                    <td>{build_id}</td>
                </tr>
""")
                f.write("""\
        </tbody>
    </table>
</body>
</html>
""")

            print(f"Report successfully generated: {OUTPUT_FILE}")

    except Error as e:
        print(f"Error: {e}")

    finally:
        if connection.is_connected():
            cursor.close()
            connection.close()

if __name__ == "__main__":
    generate_report()
